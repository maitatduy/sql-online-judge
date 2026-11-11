package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class CodeRunApiIntegrationTest extends ContainerSupport {

    private static final String RUN_REQUESTED = "sqljudge.run.requested";
    private static final String CODE = "SELECT name FROM departments";
    private static final String RUN = "{\"dialect\":\"POSTGRESQL\",\"code\":\"" + CODE + "\"}";
    private static final HttpServer PROBLEM_SERVICE = ProblemServiceStub.start();

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

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
        jdbcTemplate.update("DELETE FROM code_runs");
    }

    @Test
    @DisplayName("Chạy thử hợp lệ trả 202, lưu lần chạy đang chờ và ghi job vào outbox")
    void start_publishedProblem_isAcceptedAndQueued() {
        ResponseEntity<String> response =
                start(ProblemServiceStub.PUBLISHED_ID, RUN, TestJwts.learnerToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String runId = JsonPath.parse(response.getBody()).read("$.runId");
        Map<String, Object> outbox =
                jdbcTemplate.queryForMap("SELECT topic, message_key, payload FROM outbox_events");
        assertThat(outbox.get("topic")).isEqualTo(RUN_REQUESTED);
        assertThat(outbox.get("message_key")).isEqualTo(runId);
        DocumentContext job = JsonPath.parse((String) outbox.get("payload"));
        assertThat(job.read("$.runId", String.class)).isEqualTo(runId);
        assertThat(job.read("$.revisionId", String.class))
                .isEqualTo(ProblemServiceStub.REVISION_ID);
        assertThat(job.read("$.dialect", String.class)).isEqualTo("POSTGRESQL");
        assertThat(job.read("$.code", String.class)).isEqualTo(CODE);
        assertThat(job.read("$.version", Integer.class)).isEqualTo(1);

        DocumentContext pending = JsonPath.parse(get(runId, TestJwts.learnerToken()).getBody());
        assertThat(pending.read("$.id", String.class)).isEqualTo(runId);
        assertThat(pending.read("$.status", String.class)).isEqualTo("PENDING");
        assertThat(pending.read("$.outcome", String.class)).isNull();
        assertThat(pending.read("$.result", Object.class)).isNull();
    }

    @Test
    @DisplayName("Lần chạy đã xong trả bảng kết quả, thời gian và không lộ mã nguồn")
    void get_completedRun_returnsResult() {
        String runId = storeRun(TestJwts.LEARNER_ID);
        jdbcTemplate.update(
                "UPDATE code_runs SET status = 'COMPLETED', outcome = 'OK', runtime_ms = 6,"
                        + " result_json = ? WHERE id = UUID_TO_BIN(?)",
                "{\"columns\": [\"name\"], \"rows\": [[\"Kỹ thuật\"], [null]],"
                        + " \"isTruncated\": true}",
                runId);

        ResponseEntity<String> response = get(runId, TestJwts.learnerToken());

        DocumentContext run = JsonPath.parse(response.getBody());
        assertThat(run.read("$.status", String.class)).isEqualTo("COMPLETED");
        assertThat(run.read("$.outcome", String.class)).isEqualTo("OK");
        assertThat(run.read("$.runtimeMs", Integer.class)).isEqualTo(6);
        assertThat(run.read("$.result.columns", List.class)).containsExactly("name");
        assertThat(run.read("$.result.rows[0][0]", String.class)).isEqualTo("Kỹ thuật");
        assertThat(run.read("$.result.rows[1][0]", String.class)).isNull();
        assertThat(run.read("$.result.isTruncated", Boolean.class)).isTrue();
        assertThat(response.getBody()).doesNotContain(CODE);
    }

    @Test
    @DisplayName("Dialect không hỗ trợ trả 400, bài không có trả 404, chưa đăng nhập trả 401")
    void start_invalidRequests_areRejected() {
        ResponseEntity<String> dialect =
                start(
                        ProblemServiceStub.PUBLISHED_ID,
                        "{\"dialect\":\"MYSQL\",\"code\":\"SELECT 1\"}",
                        TestJwts.learnerToken());
        assertThat(dialect.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(dialect.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("dialect");
        assertThat(
                        start(
                                        ProblemServiceStub.PUBLISHED_ID,
                                        "{\"dialect\":\"POSTGRESQL\",\"code\":\" \"}",
                                        TestJwts.learnerToken())
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(
                        start(UUID.randomUUID().toString(), RUN, TestJwts.learnerToken())
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(start(ProblemServiceStub.PUBLISHED_ID, RUN, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM code_runs", Long.class))
                .isZero();
    }

    @Test
    @DisplayName("Lần chạy của người khác hay không tồn tại trả 404")
    void get_foreignOrUnknownRun_isNotFound() {
        String foreign = storeRun(UUID.randomUUID());

        ResponseEntity<String> response = get(foreign, TestJwts.learnerToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("CODE_RUN_NOT_FOUND");
        assertThat(get(UUID.randomUUID().toString(), TestJwts.learnerToken()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String storeRun(UUID userId) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO code_runs (id, user_id, problem_id, problem_revision_id, dialect,"
                        + " code, status, version, created_at, updated_at) VALUES"
                        + " (UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(UUID()),"
                        + " UUID_TO_BIN(UUID()), 'POSTGRESQL', ?, 'PENDING', 0, NOW(6), NOW(6))",
                id,
                userId.toString(),
                CODE);
        return id;
    }

    private ResponseEntity<String> start(String problemId, String body, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearer != null) {
            headers.setBearerAuth(bearer);
        }
        return restTemplate.exchange(
                "/api/problems/" + problemId + "/runs",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
    }

    private ResponseEntity<String> get(String runId, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        return restTemplate.exchange(
                "/api/runs/" + runId, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
