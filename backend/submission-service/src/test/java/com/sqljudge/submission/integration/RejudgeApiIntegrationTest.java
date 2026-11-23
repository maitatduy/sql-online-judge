package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.submission.entity.JudgeResult;
import com.sqljudge.submission.entity.Verdict;
import com.sqljudge.submission.service.JudgeResultService;
import com.sun.net.httpserver.HttpServer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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
class RejudgeApiIntegrationTest extends ContainerSupport {

    private static final HttpServer PROBLEM_SERVICE = ProblemServiceStub.start();
    private static final String PROBLEM_ID = ProblemServiceStub.PUBLISHED_ID;
    private static final String REJUDGE = "/api/admin/problems/" + PROBLEM_ID + "/rejudge";
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String REQUESTED = "sqljudge.submission.rejudge-requested";
    private static final String FINISHED = "sqljudge.submission.rejudge-finished";
    private static final String JUDGED = "sqljudge.submission.judged";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JudgeResultService judgeResultService;

    @DynamicPropertySource
    static void problemServiceUrl(DynamicPropertyRegistry registry) {
        registry.add(
                "sqljudge.problem-service-url",
                () -> "http://localhost:" + PROBLEM_SERVICE.getAddress().getPort());
    }

    @AfterAll
    static void stopProblemService() {
        PROBLEM_SERVICE.stop(0);
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM submissions");
        jdbcTemplate.update("DELETE FROM rejudge_batches");
    }

    @Test
    @DisplayName("Chỉ chấm lại bài nộp luyện tập đã chấm xong, theo phiên bản đang xuất bản")
    void start_requeuesCompletedPracticeSubmissions() {
        String first = store("COMPLETED", "AC", null);
        String second = store("COMPLETED", "WA", null);
        String inContest = store("COMPLETED", "AC", UUID.randomUUID().toString());
        String pending = store("PENDING", null, null);

        ResponseEntity<String> started = send(HttpMethod.POST, REJUDGE, ADMIN);

        assertThat(started.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        DocumentContext batch = JsonPath.parse(started.getBody());
        assertThat(batch.read("$.totalCount", Integer.class)).isEqualTo(2);
        assertThat(batch.read("$.status", String.class)).isEqualTo("RUNNING");
        for (String id : List.of(first, second)) {
            Map<String, Object> row = row(id);
            assertThat(row.get("status")).isEqualTo("PENDING");
            assertThat(row.get("verdict")).isNull();
            assertThat(row.get("attempts")).isEqualTo(2);
            assertThat(row.get("revision")).isEqualTo(ProblemServiceStub.REVISION_ID);
        }
        assertThat(row(inContest).get("status")).isEqualTo("COMPLETED");
        assertThat(row(pending).get("attempts")).isEqualTo(1);
        assertThat(outboxCount(REQUESTED)).isEqualTo(2);
        assertThat(code(send(HttpMethod.POST, REJUDGE, ADMIN))).isEqualTo("REJUDGE_IN_PROGRESS");
    }

    @Test
    @DisplayName(
            "Chấm xong mọi bài thì đợt kết thúc, phát rejudge-finished và judged có cờ chấm lại")
    void completion_finishesBatchOnce() {
        String first = store("COMPLETED", "AC", null);
        String second = store("COMPLETED", "WA", null);
        send(HttpMethod.POST, REJUDGE, ADMIN);

        judgeResultService.complete(UUID.fromString(first), result(2, Verdict.WA));
        assertThat(
                        JsonPath.parse(send(HttpMethod.GET, REJUDGE, ADMIN).getBody())
                                .read("$.remainingCount", Integer.class))
                .isEqualTo(1);
        judgeResultService.complete(UUID.fromString(second), result(2, Verdict.AC));
        judgeResultService.complete(UUID.fromString(second), result(2, Verdict.AC));

        DocumentContext latest = JsonPath.parse(send(HttpMethod.GET, REJUDGE, ADMIN).getBody());
        assertThat(latest.read("$.status", String.class)).isEqualTo("FINISHED");
        assertThat(latest.read("$.remainingCount", Integer.class)).isZero();
        assertThat(outboxCount(FINISHED)).isEqualTo(1);
        assertThat(
                        jdbcTemplate.queryForList(
                                "SELECT payload FROM outbox_events WHERE topic = ?",
                                String.class,
                                JUDGED))
                .hasSize(2)
                .allSatisfy(payload -> assertThat(payload).contains("\"isRejudge\":true"));
        assertThat(row(first).get("verdict")).isEqualTo("WA");
    }

    @Test
    @DisplayName("Bài không có bài nộp luyện tập thì đợt kết thúc ngay")
    void start_withoutSubmissions_finishesImmediately() {
        ResponseEntity<String> started = send(HttpMethod.POST, REJUDGE, ADMIN);

        assertThat(JsonPath.parse(started.getBody()).read("$.status", String.class))
                .isEqualTo("FINISHED");
        assertThat(outboxCount(FINISHED)).isEqualTo(1);
    }

    @Test
    @DisplayName("Tổng hợp nội bộ đếm lượt nộp, lượt AC và lần AC đầu của từng người, bỏ qua SE")
    void judgingSummary_aggregatesPerUser() {
        store("COMPLETED", "AC", null);
        store("COMPLETED", "WA", null);
        store("COMPLETED", "SE", null);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", "test-internal-token");

        DocumentContext summary =
                JsonPath.parse(
                        restTemplate
                                .exchange(
                                        "/internal/problems/" + PROBLEM_ID + "/judging-summary",
                                        HttpMethod.GET,
                                        new HttpEntity<>(headers),
                                        String.class)
                                .getBody());

        assertThat(summary.read("$.submissionCount", Integer.class)).isEqualTo(2);
        assertThat(summary.read("$.acceptedCount", Integer.class)).isEqualTo(1);
        assertThat(summary.read("$.users", List.class)).hasSize(2);
        assertThat(summary.read("$.users[?(@.firstAcceptedAt != null)]", List.class)).hasSize(1);
        assertThat(
                        restTemplate
                                .getForEntity(
                                        "/internal/problems/" + PROBLEM_ID + "/judging-summary",
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Chưa chấm lại lần nào thì 404; không phải ADMIN nhận 403")
    void latest_andPermissions() {
        assertThat(send(HttpMethod.GET, REJUDGE, ADMIN).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.POST, REJUDGE, TestJwts.learnerToken()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String store(String status, String verdict, String contestId) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO submissions (id, user_id, problem_id, problem_revision_id, dialect,"
                        + " code, status, verdict, judged_testcases, total_testcases, attempts,"
                        + " contest_id, judged_at, version, created_at, updated_at) VALUES"
                        + " (UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(UUID()),"
                        + " 'POSTGRESQL', 'SELECT 1', ?, ?, 2, 2, 1, UUID_TO_BIN(?), NOW(6), 0,"
                        + " NOW(6), NOW(6))",
                id,
                UUID.randomUUID().toString(),
                PROBLEM_ID,
                status,
                verdict,
                contestId);
        return id;
    }

    private Map<String, Object> row(String id) {
        return jdbcTemplate.queryForMap(
                "SELECT status, verdict, attempts, BIN_TO_UUID(problem_revision_id) AS revision"
                        + " FROM submissions WHERE id = UUID_TO_BIN(?)",
                id);
    }

    private int outboxCount(String topic) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE topic = ?", Integer.class, topic);
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, method, new HttpEntity<>(headers), String.class);
    }

    private static String code(ResponseEntity<String> response) {
        return JsonPath.parse(response.getBody()).read("$.code", String.class);
    }

    private static JudgeResult result(int attempt, Verdict verdict) {
        return new JudgeResult(attempt, verdict, 5, null, null, null, null, 3);
    }
}
