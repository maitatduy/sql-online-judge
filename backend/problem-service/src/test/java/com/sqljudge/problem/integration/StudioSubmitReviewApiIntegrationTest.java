package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class StudioSubmitReviewApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String STATEMENT = "{\"statementMarkdown\":\"Liệt kê tên phòng ban.\"}";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private StudioFixtures studio;
    private String problemId;
    private String sampleVariantId;
    private String hiddenVariantId;

    @BeforeEach
    void createProblemWithAnswer() {
        studio = new StudioFixtures(restTemplate, SETTER);
        problemId = studio.createProblem("Phòng ban");
        String datasetId = studio.createReadyDataset("Công ty");
        hiddenVariantId = studio.addVariant(datasetId, "Ẩn", false);
        sampleVariantId = studio.variantIdsOf(datasetId).get(0);
        studio.chooseDataset(problemId, datasetId);
        studio.saveAnswer(problemId, sampleVariantId, hiddenVariantId);
        studio.send(HttpMethod.PATCH, problemPath(), STATEMENT);
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM answer_checks");
        jdbcTemplate.update("UPDATE problems SET draft_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Lần kiểm tra gần nhất trả về kèm còn khớp đáp án hiện tại hay không")
    void latestCheck_returnsNewestAndWhetherItMatchesTheAnswer() {
        ResponseEntity<String> none = studio.send(HttpMethod.GET, latestPath(), null);
        assertThat(none.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(none.getBody()).read("$.code", String.class))
                .isEqualTo("ANSWER_CHECK_NOT_FOUND");

        startCheck();
        String newestId = startCheck();
        DocumentContext latest =
                JsonPath.parse(studio.send(HttpMethod.GET, latestPath(), null).getBody());
        assertThat(latest.read("$.id", String.class)).isEqualTo(newestId);
        assertThat(latest.read("$.isCurrent", Boolean.class)).isTrue();

        studio.saveAnswer(problemId, hiddenVariantId, sampleVariantId);
        DocumentContext stale =
                JsonPath.parse(studio.send(HttpMethod.GET, latestPath(), null).getBody());
        assertThat(stale.read("$.id", String.class)).isEqualTo(newestId);
        assertThat(stale.read("$.isCurrent", Boolean.class)).isFalse();
    }

    @Test
    @DisplayName("Kiểm tra đạt và khớp đáp án hiện tại thì gửi duyệt được, bài bị khoá sửa")
    void submitForReview_passedCurrentCheck_movesToPendingReview() {
        markPassed(startCheck());

        ResponseEntity<String> response = studio.send(HttpMethod.POST, submitPath(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.parse(response.getBody()).read("$.status", String.class))
                .isEqualTo("PENDING_REVIEW");
        assertThat(errorCodeOf(studio.send(HttpMethod.PATCH, problemPath(), STATEMENT)))
                .isEqualTo("PROBLEM_LOCKED");
        assertThat(errorCodeOf(studio.send(HttpMethod.POST, submitPath(), null)))
                .isEqualTo("PROBLEM_LOCKED");
    }

    @Test
    @DisplayName("Chưa kiểm tra, kiểm tra chưa đạt hoặc đã sửa đáp án sau đó thì không gửi được")
    void submitForReview_withoutPassedCurrentCheck_isRejected() {
        assertThat(errorCodeOf(studio.send(HttpMethod.POST, submitPath(), null)))
                .isEqualTo("PROBLEM_ANSWER_NOT_VERIFIED");

        String failedId = startCheck();
        jdbcTemplate.update(
                "UPDATE answer_checks SET status = 'FAILED' WHERE id = UUID_TO_BIN(?)", failedId);
        assertThat(errorCodeOf(studio.send(HttpMethod.POST, submitPath(), null)))
                .isEqualTo("PROBLEM_ANSWER_NOT_VERIFIED");

        markPassed(startCheck());
        studio.saveAnswer(problemId, sampleVariantId);
        ResponseEntity<String> stale = studio.send(HttpMethod.POST, submitPath(), null);
        assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(errorCodeOf(stale)).isEqualTo("PROBLEM_ANSWER_NOT_VERIFIED");
        assertThat(problemStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("Đề bài còn trống thì không gửi duyệt được")
    void submitForReview_blankStatement_isRejected() {
        markPassed(startCheck());
        studio.send(HttpMethod.PATCH, problemPath(), "{\"statementMarkdown\":\"  \"}");

        assertThat(errorCodeOf(studio.send(HttpMethod.POST, submitPath(), null)))
                .isEqualTo("PROBLEM_STATEMENT_MISSING");
        assertThat(problemStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("Học viên nhận 403, người ra đề khác nhận 404")
    void submitForReview_wrongRoleOrOwner_isRejected() {
        markPassed(startCheck());

        assertThat(
                        studio.send(HttpMethod.POST, submitPath(), null, TestJwts.learnerToken())
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(studio.send(HttpMethod.POST, submitPath(), null, OTHER_SETTER).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(studio.send(HttpMethod.GET, latestPath(), null, OTHER_SETTER).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(problemStatus()).isEqualTo("DRAFT");
    }

    private String startCheck() {
        return JsonPath.parse(
                        studio.send(HttpMethod.POST, problemPath() + "/answer-checks", null)
                                .getBody())
                .read("$.id", String.class);
    }

    private void markPassed(String checkId) {
        jdbcTemplate.update(
                "UPDATE answer_checks SET status = 'PASSED', completed_at = NOW(6)"
                        + " WHERE id = UUID_TO_BIN(?)",
                checkId);
    }

    private String problemStatus() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM problems WHERE id = UUID_TO_BIN(?)", String.class, problemId);
    }

    private static String errorCodeOf(ResponseEntity<String> response) {
        return JsonPath.parse(response.getBody()).read("$.code", String.class);
    }

    private String problemPath() {
        return "/api/studio/problems/" + problemId;
    }

    private String latestPath() {
        return problemPath() + "/answer-checks/latest";
    }

    private String submitPath() {
        return problemPath() + "/submit-for-review";
    }
}
