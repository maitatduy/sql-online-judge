package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import java.util.Map;
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
class ContestRunApiIntegrationTest extends ContainerSupport {

    private static final String RUN_REQUESTED = "sqljudge.run.requested";
    private static final String RUN =
            "{\"dialect\":\"POSTGRESQL\",\"code\":\"SELECT name FROM departments\"}";
    private static final HttpServer PROBLEM_SERVICE = ProblemServiceStub.start();
    private static final HttpServer CONTEST_SERVICE = ContestServiceStub.start();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void serviceUrls(DynamicPropertyRegistry registry) {
        registry.add(
                "sqljudge.problem-service-url",
                () -> "http://localhost:" + PROBLEM_SERVICE.getAddress().getPort());
        registry.add(
                "sqljudge.contest-service-url",
                () -> "http://localhost:" + CONTEST_SERVICE.getAddress().getPort());
    }

    @AfterAll
    static void stopStubs() {
        PROBLEM_SERVICE.stop(0);
        CONTEST_SERVICE.stop(0);
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM code_runs");
    }

    @Test
    @DisplayName("Chạy thử trong contest trả 202 và ghi job chạy trên bản đề của bài contest")
    void run_inContest_isQueued() {
        ResponseEntity<String> response = start("sql-cup", "A");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String runId = JsonPath.parse(response.getBody()).read("$.runId");
        Map<String, Object> outbox =
                jdbcTemplate.queryForMap("SELECT topic, message_key, payload FROM outbox_events");
        assertThat(outbox.get("topic")).isEqualTo(RUN_REQUESTED);
        assertThat(outbox.get("message_key")).isEqualTo(runId);
        DocumentContext job = JsonPath.parse((String) outbox.get("payload"));
        assertThat(job.read("$.revisionId", String.class))
                .isEqualTo(ProblemServiceStub.REVISION_ID);
    }

    @Test
    @DisplayName("Chưa đăng ký hoặc contest đã kết thúc thì không chạy thử được")
    void run_ineligible_keepsContestErrorCodes() {
        ResponseEntity<String> outsider = start("sql-cup", "B");
        ResponseEntity<String> finished = start("da-xong", "A");

        assertThat(outsider.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(JsonPath.parse(outsider.getBody()).read("$.code", String.class))
                .isEqualTo("CONTEST_NOT_REGISTERED");
        assertThat(finished.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM code_runs", Long.class))
                .isZero();
    }

    private ResponseEntity<String> start(String slug, String letter) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwts.learnerToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                "/api/contests/" + slug + "/problems/" + letter + "/runs",
                HttpMethod.POST,
                new HttpEntity<>(RUN, headers),
                String.class);
    }
}
