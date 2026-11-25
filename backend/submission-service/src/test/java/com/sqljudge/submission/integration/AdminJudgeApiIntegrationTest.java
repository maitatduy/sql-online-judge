package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.common.persistence.UuidV7;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class AdminJudgeApiIntegrationTest extends ContainerSupport {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID MINH = UUID.randomUUID();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void authService(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.auth-service-url", AuthServiceStub::url);
    }

    @BeforeEach
    void users() {
        AuthServiceStub.reset();
        AuthServiceStub.user(MINH, "minh.anh");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM submissions");
        jdbcTemplate.update("DELETE FROM outbox_events");
    }

    @Test
    @DisplayName("Hàng đợi đếm bài chờ, đang chấm, bị kẹt, lỗi SE trong ngày và đã chấm trong giờ")
    void queue_countsJudgingState() {
        store("PENDING", null, 0, null);
        store("PENDING", null, 10L * 60, null);
        store("JUDGING", null, 0, null);
        store("COMPLETED", "SE", 0, 2L * 3600);
        store("COMPLETED", "SE", 0, 3L * 86400);
        store("COMPLETED", "AC", 0, 10L * 60);

        DocumentContext queue = getOk("/api/admin/judge/queue");

        assertThat(queue.read("$.waiting", Integer.class)).isEqualTo(2);
        assertThat(queue.read("$.judging", Integer.class)).isEqualTo(1);
        assertThat(queue.read("$.stuck", Integer.class)).isEqualTo(1);
        assertThat(queue.read("$.systemErrorsLastDay", Integer.class)).isEqualTo(1);
        assertThat(queue.read("$.judgedLastHour", Integer.class)).isEqualTo(1);
        assertThat(queue.read("$.stuckAfterMinutes", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("Cần xử lý gồm bài kẹt và bài SE 7 ngày qua, kèm username")
    void attention_listsStuckAndSystemErrors() {
        UUID stuck = store("PENDING", null, 10L * 60, null);
        store("PENDING", null, 0, null);
        UUID error = store("COMPLETED", "SE", 0, 1L * 86400);
        store("COMPLETED", "SE", 0, 9L * 86400);
        store("COMPLETED", "WA", 0, 0L);

        DocumentContext attention = getOk("/api/admin/judge/attention");

        assertThat(attention.read("$.items[*].id", List.class))
                .containsExactlyInAnyOrder(stuck.toString(), error.toString());
        assertThat(attention.read("$.items[0].username", String.class)).isEqualTo("minh.anh");
    }

    @Test
    @DisplayName(
            "Chấm lại bài kẹt: về chờ chấm, tăng lần chấm, phát job chấm lại; bài bình thường 422")
    void rejudge_requeuesOnlyStuckOrSystemError() {
        UUID stuck = store("JUDGING", null, 10L * 60, null);
        UUID error = store("COMPLETED", "SE", 0, 0L);
        UUID accepted = store("COMPLETED", "AC", 0, 0L);
        UUID fresh = store("PENDING", null, 0, null);

        ResponseEntity<String> stuckResponse = post("/api/admin/submissions/" + stuck + "/rejudge");
        ResponseEntity<String> errorResponse = post("/api/admin/submissions/" + error + "/rejudge");

        assertThat(stuckResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        Map<String, Object> requeued =
                jdbcTemplate.queryForMap(
                        "SELECT status, verdict, attempts FROM submissions WHERE id ="
                                + " UUID_TO_BIN(?)",
                        error.toString());
        assertThat(requeued)
                .containsEntry("status", "PENDING")
                .containsEntry("verdict", null)
                .containsEntry("attempts", 2);
        assertThat(
                        jdbcTemplate.queryForList(
                                "SELECT message_key FROM outbox_events WHERE topic = ?",
                                String.class,
                                "sqljudge.submission.rejudge-requested"))
                .containsExactlyInAnyOrder(stuck.toString(), error.toString());
        assertThat(post("/api/admin/submissions/" + accepted + "/rejudge").getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(post("/api/admin/submissions/" + fresh + "/rejudge").getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(post("/api/admin/submissions/" + UUID.randomUUID() + "/rejudge").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Học viên bị 403 với hàng đợi và với chấm lại")
    void judgeEndpoints_requireAdmin() {
        UUID error = store("COMPLETED", "SE", 0, 0L);

        assertThat(exchange(HttpMethod.GET, "/api/admin/judge/queue", TestJwts.learnerToken()))
                .extracting(ResponseEntity::getStatusCode)
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(
                        exchange(
                                HttpMethod.POST,
                                "/api/admin/submissions/" + error + "/rejudge",
                                TestJwts.learnerToken()))
                .extracting(ResponseEntity::getStatusCode)
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private UUID store(
            String status, String verdict, long updatedSecondsAgo, Long judgedSecondsAgo) {
        UUID id = UuidV7.generate();
        jdbcTemplate.update(
                "INSERT INTO submissions (id, user_id, problem_id, problem_revision_id,"
                        + " problem_number, problem_slug, problem_title, dialect, code, status,"
                        + " verdict, judged_testcases, total_testcases, attempts, version,"
                        + " created_at, updated_at, judged_at) VALUES (UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(UUID()), UUID_TO_BIN(UUID()), 12,"
                        + " 'tim-luong', 'Tìm lương', 'POSTGRESQL', 'SELECT 1', ?, ?, 0, 2, 1, 0,"
                        + " UTC_TIMESTAMP(6) - INTERVAL ? SECOND,"
                        + " UTC_TIMESTAMP(6) - INTERVAL ? SECOND,"
                        + " IF(? IS NULL, NULL, UTC_TIMESTAMP(6) - INTERVAL ? SECOND))",
                id.toString(),
                MINH.toString(),
                status,
                verdict,
                updatedSecondsAgo,
                updatedSecondsAgo,
                judgedSecondsAgo,
                judgedSecondsAgo);
        return id;
    }

    private DocumentContext getOk(String path) {
        ResponseEntity<String> response = exchange(HttpMethod.GET, path, adminToken());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody());
    }

    private ResponseEntity<String> post(String path) {
        return exchange(HttpMethod.POST, path, adminToken());
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, method, new HttpEntity<>(headers), String.class);
    }

    private static String adminToken() {
        return TestJwts.token(ADMIN_ID, "ADMIN");
    }
}
