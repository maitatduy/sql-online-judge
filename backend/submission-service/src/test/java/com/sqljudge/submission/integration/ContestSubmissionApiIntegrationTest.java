package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.submission.entity.JudgeResult;
import com.sqljudge.submission.entity.ResultDiff;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class ContestSubmissionApiIntegrationTest extends ContainerSupport {

    private static final String SOLUTION =
            "{\"dialect\":\"POSTGRESQL\",\"code\":\"SELECT name FROM departments\"}";
    private static final String JUDGED_TOPIC = "sqljudge.submission.judged";
    private static final HttpServer PROBLEM_SERVICE = ProblemServiceStub.start();
    private static final HttpServer CONTEST_SERVICE = ContestServiceStub.start();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JudgeResultService judgeResultService;

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
        jdbcTemplate.update("DELETE FROM submissions");
    }

    @Test
    @DisplayName("Nộp bài trong contest lưu id contest, không lưu số và slug của bài gốc")
    void submit_inContest_storesContestId() {
        ResponseEntity<String> response = submit("sql-cup", "A");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String submissionId = JsonPath.parse(response.getBody()).read("$.submissionId");
        Map<String, Object> stored =
                jdbcTemplate.queryForMap(
                        "SELECT BIN_TO_UUID(contest_id) AS contest_id,"
                                + " BIN_TO_UUID(problem_id) AS problem_id, problem_number,"
                                + " problem_slug FROM submissions WHERE id = UUID_TO_BIN(?)",
                        submissionId);
        assertThat(stored)
                .containsEntry("contest_id", ContestServiceStub.CONTEST_ID)
                .containsEntry("problem_id", ProblemServiceStub.PUBLISHED_ID)
                .containsEntry("problem_number", null)
                .containsEntry("problem_slug", null);
    }

    @Test
    @DisplayName("Chấm xong bài trong contest thì không lộ diff, event kèm id contest và giờ nộp")
    void judged_inContest_hidesDiffAndCarriesContest() {
        String submissionId =
                JsonPath.parse(submit("sql-cup", "A").getBody()).read("$.submissionId");
        ResultDiff diff =
                new ResultDiff(
                        ResultDiff.MismatchReason.ROWS,
                        List.of("name"),
                        List.of("name"),
                        1,
                        1,
                        List.of(),
                        List.of(),
                        null);

        judgeResultService.complete(
                UUID.fromString(submissionId),
                new JudgeResult(1, Verdict.WA, 12, 1, false, null, diff, 1));

        DocumentContext submission =
                JsonPath.parse(
                        send(HttpMethod.GET, "/api/submissions/" + submissionId, null).getBody());
        assertThat(submission.read("$.verdict", String.class)).isEqualTo("WA");
        assertThat(submission.read("$.contestId", String.class))
                .isEqualTo(ContestServiceStub.CONTEST_ID);
        assertThat(submission.read("$.firstFailedTestcase.index", Integer.class)).isEqualTo(1);
        assertThat(submission.read("$.firstFailedTestcase.diff", Object.class)).isNull();
        DocumentContext event =
                JsonPath.parse(
                        jdbcTemplate.queryForObject(
                                "SELECT payload FROM outbox_events WHERE topic = ?",
                                String.class,
                                JUDGED_TOPIC));
        assertThat(event.read("$.contestId", String.class))
                .isEqualTo(ContestServiceStub.CONTEST_ID);
        assertThat(event.read("$.submittedAt", String.class)).isNotBlank();
    }

    @Test
    @DisplayName("Lỗi từ contest-service được trả nguyên mã cho người dùng")
    void submit_ineligible_keepsContestErrorCodes() {
        ResponseEntity<String> outsider = submit("sql-cup", "B");
        ResponseEntity<String> finished = submit("da-xong", "A");
        ResponseEntity<String> unknown = submit("khong-co", "A");
        ResponseEntity<String> broken = submit("loi", "A");

        assertThat(outsider.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(JsonPath.parse(outsider.getBody()).read("$.code", String.class))
                .isEqualTo("CONTEST_NOT_REGISTERED");
        assertThat(finished.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(JsonPath.parse(finished.getBody()).read("$.code", String.class))
                .isEqualTo("CONTEST_ALREADY_FINISHED");
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(broken.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM submissions", Long.class))
                .isZero();
    }

    private ResponseEntity<String> submit(String slug, String letter) {
        return send(
                HttpMethod.POST,
                "/api/contests/" + slug + "/problems/" + letter + "/submissions",
                SOLUTION);
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwts.learnerToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
