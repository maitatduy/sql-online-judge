package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.common.persistence.UuidV7;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class AdminSubmissionApiIntegrationTest extends ContainerSupport {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID MINH = UUID.randomUUID();
    private static final UUID KHOA = UUID.randomUUID();

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
        AuthServiceStub.user(KHOA, "khoa.tran");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM submissions");
    }

    @Test
    @DisplayName(
            "ADMIN xem mọi bài nộp mới nhất trước, kèm username; lọc theo verdict, nguồn, người")
    void search_listsAndFiltersSubmissions() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UUID oldest = store(MINH, "WA", "COMPLETED", now.minus(3, ChronoUnit.HOURS));
        UUID contest = store(KHOA, "AC", "COMPLETED", now.minus(2, ChronoUnit.HOURS));
        jdbcTemplate.update(
                "UPDATE submissions SET contest_id = UUID_TO_BIN(UUID()) WHERE id = UUID_TO_BIN(?)",
                contest.toString());
        UUID pending = store(MINH, null, "PENDING", now.minus(1, ChronoUnit.HOURS));
        UUID deleted = store(MINH, "AC", "COMPLETED", now);
        jdbcTemplate.update(
                "UPDATE submissions SET deleted_at = NOW(6) WHERE id = UUID_TO_BIN(?)",
                deleted.toString());

        DocumentContext all = getOk("/api/admin/submissions?size=2");
        DocumentContext secondPage = getOk("/api/admin/submissions?size=2&page=2");

        assertThat(all.read("$.items[*].id", List.class))
                .containsExactly(pending.toString(), contest.toString());
        assertThat(all.read("$.items[0].username", String.class)).isEqualTo("minh.anh");
        assertThat(all.read("$.items[0].status", String.class)).isEqualTo("PENDING");
        assertThat(all.read("$.items[1].contestId", String.class)).isNotNull();
        assertThat(all.read("$.totalItems", Integer.class)).isEqualTo(3);
        assertThat(all.read("$.items[0]", Map.class)).doesNotContainKey("code");
        assertThat(secondPage.read("$.items[*].id", List.class)).containsExactly(oldest.toString());
        assertThat(ids("/api/admin/submissions?verdict=WA")).containsExactly(oldest.toString());
        assertThat(ids("/api/admin/submissions?status=PENDING"))
                .containsExactly(pending.toString());
        assertThat(ids("/api/admin/submissions?source=CONTEST"))
                .containsExactly(contest.toString());
        assertThat(ids("/api/admin/submissions?source=PRACTICE"))
                .containsExactly(pending.toString(), oldest.toString());
        assertThat(ids("/api/admin/submissions?username=khoa.tran"))
                .containsExactly(contest.toString());
        assertThat(ids("/api/admin/submissions?username=khong.co")).isEmpty();
        assertThat(ids("/api/admin/submissions?problem=tim-luong&dialect=POSTGRESQL")).hasSize(3);
    }

    @Test
    @DisplayName("ADMIN xem chi tiết kèm code; bài đã xoá trả 404")
    void detail_returnsCodeForAdmin() {
        UUID id = store(MINH, "RE", "COMPLETED", Instant.now());
        jdbcTemplate.update(
                "UPDATE submissions SET error_message = 'Lỗi cú pháp' WHERE id = UUID_TO_BIN(?)",
                id.toString());

        DocumentContext detail = getOk("/api/admin/submissions/" + id);

        assertThat(detail.read("$.code", String.class)).isEqualTo("SELECT 1");
        assertThat(detail.read("$.username", String.class)).isEqualTo("minh.anh");
        assertThat(detail.read("$.errorMessage", String.class)).isEqualTo("Lỗi cú pháp");
        assertThat(detail.read("$.totalTestcases", Integer.class)).isEqualTo(2);
        assertThat(get("/api/admin/submissions/" + UUID.randomUUID(), adminToken()))
                .extracting(ResponseEntity::getStatusCode)
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Học viên bị 403, khách bị 401, kích thước trang quá lớn bị 400")
    void search_requiresAdmin() {
        assertThat(get("/api/admin/submissions", TestJwts.learnerToken()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/admin/submissions", null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/admin/submissions?size=101", adminToken()).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private UUID store(UUID userId, String verdict, String status, Instant createdAt) {
        UUID id = UuidV7.generate(createdAt.toEpochMilli());
        jdbcTemplate.update(
                "INSERT INTO submissions (id, user_id, problem_id, problem_revision_id,"
                        + " problem_number, problem_slug, problem_title, dialect, code, status,"
                        + " verdict, runtime_ms, judged_testcases, total_testcases, attempts,"
                        + " version, created_at, updated_at) VALUES (UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(UUID()), UUID_TO_BIN(UUID()), 12,"
                        + " 'tim-luong', 'Tìm lương', 'POSTGRESQL', 'SELECT 1', ?, ?, 15, 2, 2, 1,"
                        + " 0, ?, NOW(6))",
                id.toString(),
                userId.toString(),
                status,
                verdict,
                Timestamp.from(createdAt));
        return id;
    }

    private List<String> ids(String path) {
        return getOk(path).read("$.items[*].id", List.class);
    }

    private DocumentContext getOk(String path) {
        ResponseEntity<String> response = get(path, adminToken());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody());
    }

    private ResponseEntity<String> get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private static String adminToken() {
        return TestJwts.token(ADMIN_ID, "ADMIN");
    }
}
