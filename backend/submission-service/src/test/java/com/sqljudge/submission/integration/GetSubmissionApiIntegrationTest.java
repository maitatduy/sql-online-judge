package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class GetSubmissionApiIntegrationTest extends ContainerSupport {

    private static final String PROBLEM_ID = "0192f3d0-0000-7000-8000-000000000005";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM submissions");
    }

    @Test
    @DisplayName("Chủ bài nộp xem được trạng thái, tiến độ và thời điểm nộp")
    void get_ownSubmission_returnsProgress() {
        String id = storeSubmission(TestJwts.LEARNER_ID, "JUDGING", null, 2);

        ResponseEntity<String> response = get(id, TestJwts.learnerToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext submission = JsonPath.parse(response.getBody());
        assertThat(submission.read("$.id", String.class)).isEqualTo(id);
        assertThat(submission.read("$.problemId", String.class)).isEqualTo(PROBLEM_ID);
        assertThat(submission.read("$.dialect", String.class)).isEqualTo("POSTGRESQL");
        assertThat(submission.read("$.status", String.class)).isEqualTo("JUDGING");
        assertThat(submission.read("$.verdict", String.class)).isNull();
        assertThat(submission.read("$.judgedTestcases", Integer.class)).isEqualTo(2);
        assertThat(submission.read("$.totalTestcases", Integer.class)).isEqualTo(3);
        assertThat(submission.read("$.firstFailedTestcase", Object.class)).isNull();
        assertThat(submission.read("$.submittedAt", String.class)).isNotBlank();
        assertThat(response.getBody()).doesNotContain("SELECT name FROM departments");
    }

    @Test
    @DisplayName("Bài nộp đã chấm trả verdict, thời gian chạy và testcase sai đầu tiên")
    void get_judgedSubmission_returnsVerdict() {
        String id = storeSubmission(TestJwts.LEARNER_ID, "COMPLETED", "WA", 3);
        jdbcTemplate.update(
                "UPDATE submissions SET runtime_ms = 12, first_failed_index = 2,"
                        + " first_failed_is_hidden = TRUE WHERE id = UUID_TO_BIN(?)",
                id);

        DocumentContext submission = JsonPath.parse(get(id, TestJwts.learnerToken()).getBody());

        assertThat(submission.read("$.verdict", String.class)).isEqualTo("WA");
        assertThat(submission.read("$.runtimeMs", Integer.class)).isEqualTo(12);
        assertThat(submission.read("$.firstFailedTestcase.index", Integer.class)).isEqualTo(2);
        assertThat(submission.read("$.firstFailedTestcase.isHidden", Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("Sai ở testcase công khai thì trả diff; ở testcase ẩn thì không bao giờ trả diff")
    void get_failedSubmission_returnsDiffOnlyForPublicTestcase() {
        String onPublic = storeFailure(false);
        String onHidden = storeFailure(true);

        DocumentContext publicFailure =
                JsonPath.parse(get(onPublic, TestJwts.learnerToken()).getBody());
        assertThat(publicFailure.read("$.firstFailedTestcase.diff.reason", String.class))
                .isEqualTo("ROWS");
        assertThat(
                        publicFailure.read(
                                "$.firstFailedTestcase.diff.missingRows[0][0].value", String.class))
                .isEqualTo("Kinh doanh");
        assertThat(publicFailure.read("$.firstFailedTestcase.diff.expectedRowCount", Integer.class))
                .isEqualTo(2);

        ResponseEntity<String> hiddenFailure = get(onHidden, TestJwts.learnerToken());
        assertThat(
                        JsonPath.parse(hiddenFailure.getBody())
                                .read("$.firstFailedTestcase.diff", Object.class))
                .isNull();
        assertThat(hiddenFailure.getBody()).doesNotContain("Kinh doanh");
    }

    @Test
    @DisplayName(
            "Bài nộp của người khác, đã xoá hoặc không tồn tại trả 404; chưa đăng nhập trả 401")
    void get_foreignDeletedOrUnknown_isNotFound() {
        String foreign = storeSubmission(UUID.randomUUID(), "PENDING", null, 0);
        String deleted = storeSubmission(TestJwts.LEARNER_ID, "PENDING", null, 0);
        jdbcTemplate.update(
                "UPDATE submissions SET deleted_at = NOW(6) WHERE id = UUID_TO_BIN(?)", deleted);

        ResponseEntity<String> response = get(foreign, TestJwts.learnerToken());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("SUBMISSION_NOT_FOUND");
        assertThat(get(deleted, TestJwts.learnerToken()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get(UUID.randomUUID().toString(), TestJwts.learnerToken()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get(foreign, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String storeSubmission(UUID userId, String status, String verdict, int judged) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO submissions (id, user_id, problem_id, problem_revision_id, dialect,"
                        + " code, status, verdict, judged_testcases, total_testcases, attempts,"
                        + " version, created_at, updated_at) VALUES (UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(UUID()), 'POSTGRESQL',"
                        + " 'SELECT name FROM departments', ?, ?, ?, 3, 1, 0, NOW(6), NOW(6))",
                id,
                userId.toString(),
                PROBLEM_ID,
                status,
                verdict,
                judged);
        return id;
    }

    private String storeFailure(boolean isHidden) {
        String id = storeSubmission(TestJwts.LEARNER_ID, "COMPLETED", "WA", 1);
        jdbcTemplate.update(
                "UPDATE submissions SET first_failed_index = 1, first_failed_is_hidden = ?,"
                        + " diff_json = ? WHERE id = UUID_TO_BIN(?)",
                isHidden,
                """
                {"reason": "ROWS", "expectedColumns": ["name"], "actualColumns": ["name"],
                 "expectedRowCount": 2, "actualRowCount": 1,
                 "missingRows": [[{"kind": "TEXT", "value": "Kinh doanh"}]],
                 "extraRows": [], "orderMismatch": null}
                """,
                id);
        return id;
    }

    private ResponseEntity<String> get(String id, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        if (bearer != null) {
            headers.setBearerAuth(bearer);
        }
        return restTemplate.exchange(
                "/api/submissions/" + id, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
