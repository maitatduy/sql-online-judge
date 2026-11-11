package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
class SubmitSolutionApiIntegrationTest extends ContainerSupport {

    private static final String PUBLISHED_ID = "0192f3d0-0000-7000-8000-000000000005";
    private static final String REVISION_ID = "0192f3d1-0000-7000-8000-000000000005";
    private static final String BROKEN_ID = "0192f3d0-0000-7000-8000-00000000000b";
    private static final String CREATED_TOPIC = "sqljudge.submission.created";
    private static final String SOLUTION =
            "{\"dialect\":\"POSTGRESQL\",\"code\":\"SELECT name FROM departments\"}";
    private static final HttpServer PROBLEM_SERVICE = startProblemService();

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
        jdbcTemplate.update("DELETE FROM submissions");
    }

    @Test
    @DisplayName("Nộp bài hợp lệ trả 202, lưu bài nộp PENDING và ghi job chấm vào outbox")
    void submit_publishedProblem_isAcceptedAndQueued() {
        ResponseEntity<String> response =
                submit(PUBLISHED_ID, SOLUTION, TestJwts.learnerToken(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String submissionId = JsonPath.parse(response.getBody()).read("$.submissionId");
        Map<String, Object> stored =
                jdbcTemplate.queryForMap(
                        "SELECT BIN_TO_UUID(user_id) AS user_id,"
                                + " BIN_TO_UUID(problem_revision_id) AS revision_id, dialect,"
                                + " status, total_testcases, attempts FROM submissions"
                                + " WHERE id = UUID_TO_BIN(?)",
                        submissionId);
        assertThat(stored)
                .containsEntry("user_id", TestJwts.LEARNER_ID.toString())
                .containsEntry("revision_id", REVISION_ID)
                .containsEntry("dialect", "POSTGRESQL")
                .containsEntry("status", "PENDING")
                .containsEntry("total_testcases", 3)
                .containsEntry("attempts", 1);
        Map<String, Object> outbox =
                jdbcTemplate.queryForMap("SELECT topic, message_key, payload FROM outbox_events");
        assertThat(outbox.get("topic")).isEqualTo(CREATED_TOPIC);
        assertThat(outbox.get("message_key")).isEqualTo(submissionId);
        DocumentContext job = JsonPath.parse((String) outbox.get("payload"));
        assertThat(job.read("$.submissionId", String.class)).isEqualTo(submissionId);
        assertThat(job.read("$.revisionId", String.class)).isEqualTo(REVISION_ID);
        assertThat(job.read("$.dialect", String.class)).isEqualTo("POSTGRESQL");
        assertThat(job.read("$.code", String.class)).isEqualTo("SELECT name FROM departments");
        assertThat(job.read("$.attempt", Integer.class)).isEqualTo(1);
        assertThat(job.read("$.version", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Gửi lại cùng Idempotency-Key thì trả lại bài nộp cũ, không tạo bài mới")
    void submit_sameIdempotencyKey_returnsSameSubmission() {
        String first =
                JsonPath.parse(
                                submit(PUBLISHED_ID, SOLUTION, TestJwts.learnerToken(), "key-1")
                                        .getBody())
                        .read("$.submissionId");
        ResponseEntity<String> again =
                submit(PUBLISHED_ID, SOLUTION, TestJwts.learnerToken(), "key-1");

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(JsonPath.parse(again.getBody()).read("$.submissionId", String.class))
                .isEqualTo(first);
        assertThat(count("submissions")).isEqualTo(1);
        assertThat(count("outbox_events")).isEqualTo(1);
    }

    @Test
    @DisplayName("Dialect bài không hỗ trợ, code trống hoặc quá 64 KB thì trả 400")
    void submit_invalidSolution_isRejected() {
        ResponseEntity<String> dialect =
                submit(
                        PUBLISHED_ID,
                        "{\"dialect\":\"MYSQL\",\"code\":\"SELECT 1\"}",
                        TestJwts.learnerToken(),
                        null);
        assertThat(dialect.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(dialect.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("dialect");

        assertThat(
                        submit(
                                        PUBLISHED_ID,
                                        "{\"dialect\":\"POSTGRESQL\",\"code\":\"  \"}",
                                        TestJwts.learnerToken(),
                                        null)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        String tooLong = "SELECT 1 -- " + "x".repeat(65_536);
        assertThat(
                        submit(
                                        PUBLISHED_ID,
                                        "{\"dialect\":\"POSTGRESQL\",\"code\":\"" + tooLong + "\"}",
                                        TestJwts.learnerToken(),
                                        null)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(count("submissions")).isZero();
    }

    @Test
    @DisplayName("Bài chưa xuất bản trả 404, problem-service lỗi trả 503, chưa đăng nhập trả 401")
    void submit_unknownProblemOrUpstreamFailureOrAnonymous_isRejected() {
        ResponseEntity<String> unknown =
                submit(UUID.randomUUID().toString(), SOLUTION, TestJwts.learnerToken(), null);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(unknown.getBody()).read("$.code", String.class))
                .isEqualTo("PROBLEM_NOT_FOUND");

        ResponseEntity<String> broken = submit(BROKEN_ID, SOLUTION, TestJwts.learnerToken(), null);
        assertThat(broken.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(JsonPath.parse(broken.getBody()).read("$.code", String.class))
                .isEqualTo("PROBLEM_SERVICE_UNAVAILABLE");

        assertThat(submit(PUBLISHED_ID, SOLUTION, null, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(count("submissions")).isZero();
    }

    private ResponseEntity<String> submit(
            String problemId, String body, String bearer, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearer != null) {
            headers.setBearerAuth(bearer);
        }
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        return restTemplate.exchange(
                "/api/problems/" + problemId + "/submissions",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private static HttpServer startProblemService() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    "/internal/problems/",
                    exchange -> {
                        String path = exchange.getRequestURI().getPath();
                        if (!"test-internal-token"
                                .equals(
                                        exchange.getRequestHeaders()
                                                .getFirst("X-Internal-Token"))) {
                            respond(exchange, 401, "{\"code\":\"UNAUTHORIZED\"}");
                        } else if (path.contains(PUBLISHED_ID)) {
                            respond(
                                    exchange,
                                    200,
                                    "{\"problemId\":\""
                                            + PUBLISHED_ID
                                            + "\",\"revisionId\":\""
                                            + REVISION_ID
                                            + "\",\"dialects\":[\"POSTGRESQL\",\"SQLITE\"],"
                                            + "\"testcaseCount\":3,\"timeLimitMs\":2000}");
                        } else if (path.contains(BROKEN_ID)) {
                            respond(exchange, 500, "{\"code\":\"INTERNAL_ERROR\"}");
                        } else {
                            respond(exchange, 404, "{\"code\":\"PROBLEM_NOT_FOUND\"}");
                        }
                    });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void respond(
            com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
