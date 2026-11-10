package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class AdminProblemReviewApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String MODERATOR = TestJwts.token(UUID.randomUUID(), "MODERATOR");
    private static final String PUBLISHED_TOPIC = "sqljudge.problem.published";
    private static final String EXPECTED_ROWS =
            "[[{\"kind\":\"TEXT\",\"value\":\"Kỹ thuật\"},{\"kind\":\"NULL\",\"value\":null}]]";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private StudioFixtures studio;
    private String problemId;

    @BeforeEach
    void submitProblemForReview() {
        studio = new StudioFixtures(restTemplate, SETTER);
        problemId = studio.createProblem("Phòng ban");
        String datasetId = studio.createReadyDataset("Công ty");
        String hiddenVariantId = studio.addVariant(datasetId, "Ẩn", false);
        studio.chooseDataset(problemId, datasetId);
        studio.saveAnswer(problemId, studio.variantIdsOf(datasetId).get(0), hiddenVariantId);
        studio.send(
                HttpMethod.PATCH,
                problemPath(),
                "{\"statementMarkdown\":\"Liệt kê tên phòng ban.\"}");
        String checkId =
                JsonPath.parse(
                                studio.send(HttpMethod.POST, problemPath() + "/answer-checks", null)
                                        .getBody())
                        .read("$.id", String.class);
        jdbcTemplate.update(
                "UPDATE answer_checks SET status = 'PASSED', completed_at = NOW(6)"
                        + " WHERE id = UUID_TO_BIN(?)",
                checkId);
        storeExpectedResults();
        studio.send(HttpMethod.POST, problemPath() + "/submit-for-review", null);
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM answer_checks");
        jdbcTemplate.update(
                "UPDATE problems SET draft_revision_id = NULL, published_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Admin duyệt thì bài được xuất bản, có số thứ tự, phát event và hiện công khai")
    void approve_pendingProblem_publishesIt() {
        ResponseEntity<String> response = review("approve", null, ADMIN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext problem = JsonPath.parse(response.getBody());
        assertThat(problem.read("$.status", String.class)).isEqualTo("PUBLISHED");
        assertThat(problem.read("$.number", Integer.class)).isPositive();
        assertThat(problem.read("$.latestReview.decision", String.class)).isEqualTo("APPROVED");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT r.frozen_at IS NOT NULL AND r.checksum IS NOT NULL"
                                        + " FROM problems p JOIN problem_revisions r"
                                        + " ON r.id = p.published_revision_id"
                                        + " WHERE p.id = UUID_TO_BIN(?)",
                                Boolean.class,
                                problemId))
                .isTrue();
        Map<String, Object> outbox =
                jdbcTemplate.queryForMap(
                        "SELECT message_key, payload FROM outbox_events WHERE topic = ?",
                        PUBLISHED_TOPIC);
        assertThat(outbox.get("message_key")).isEqualTo(problemId);
        DocumentContext event = JsonPath.parse((String) outbox.get("payload"));
        assertThat(event.read("$.problemId", String.class)).isEqualTo(problemId);
        assertThat(event.read("$.slug", String.class)).isEqualTo(problem.read("$.slug"));

        DocumentContext detail =
                JsonPath.parse(
                        restTemplate.getForObject(
                                "/api/problems/" + problem.read("$.slug", String.class),
                                String.class));
        assertThat(detail.read("$.number", Integer.class))
                .isEqualTo(problem.read("$.number", Integer.class));
        assertThat(detail.read("$.examples[0].rows", List.class))
                .containsExactly(java.util.Arrays.asList("Kỹ thuật", null));
        assertThat(detail.read("$.hiddenTestcaseCount", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Admin yêu cầu sửa kèm lý do thì người ra đề thấy lý do và sửa tiếp được")
    void requestChanges_withReason_reopensTheDraft() {
        ResponseEntity<String> response =
                review("request-changes", "Đề bài cần thêm ví dụ.", ADMIN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext problem =
                JsonPath.parse(studio.send(HttpMethod.GET, problemPath(), null).getBody());
        assertThat(problem.read("$.status", String.class)).isEqualTo("CHANGES_REQUESTED");
        assertThat(problem.read("$.latestReview.decision", String.class))
                .isEqualTo("CHANGES_REQUESTED");
        assertThat(problem.read("$.latestReview.reason", String.class))
                .isEqualTo("Đề bài cần thêm ví dụ.");
        assertThat(problem.read("$.latestReview.reviewedAt", String.class)).isNotBlank();
        assertThat(
                        studio.send(
                                        HttpMethod.PATCH,
                                        problemPath(),
                                        "{\"title\":\"Phòng ban mới\"}")
                                .getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(publishedEventCount()).isZero();
    }

    @Test
    @DisplayName("Từ chối cần lý do; bài bị từ chối không duyệt lại được")
    void reject_requiresReasonAndIsFinal() {
        ResponseEntity<String> blank = review("reject", " ", ADMIN);
        assertThat(blank.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<String> rejected = review("reject", "Trùng với bài 12.", ADMIN);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.parse(rejected.getBody()).read("$.status", String.class))
                .isEqualTo("REJECTED");

        ResponseEntity<String> again = review("approve", null, ADMIN);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(JsonPath.parse(again.getBody()).read("$.code", String.class))
                .isEqualTo("PROBLEM_NOT_PENDING_REVIEW");
        assertThat(publishedEventCount()).isZero();
    }

    @Test
    @DisplayName("Chỉ ADMIN duyệt được: SETTER, MODERATOR nhận 403, bài lạ nhận 404")
    void review_nonAdminOrUnknownProblem_isRejected() {
        assertThat(review("approve", null, SETTER).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(review("approve", null, MODERATOR).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(
                        studio.send(
                                        HttpMethod.POST,
                                        "/api/admin/problems/" + UUID.randomUUID() + "/approve",
                                        null,
                                        ADMIN)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT status FROM problems WHERE id = UUID_TO_BIN(?)",
                                String.class,
                                problemId))
                .isEqualTo("PENDING_REVIEW");
    }

    private ResponseEntity<String> review(String action, String reason, String bearer) {
        String body = reason == null ? null : "{\"reason\":\"" + reason + "\"}";
        return studio.send(
                HttpMethod.POST, "/api/admin/problems/" + problemId + "/" + action, body, bearer);
    }

    private void storeExpectedResults() {
        jdbcTemplate.update(
                "INSERT INTO expected_results (id, testcase_id, columns_json, rows_json,"
                        + " row_count, checksum, created_at, updated_at)"
                        + " SELECT UUID_TO_BIN(UUID()), t.id, '[\"name\",\"note\"]', ?, 1,"
                        + " REPEAT('0', 64), NOW(6), NOW(6)"
                        + " FROM testcases t JOIN problems p ON p.draft_revision_id = t.revision_id"
                        + " WHERE p.id = UUID_TO_BIN(?)",
                EXPECTED_ROWS,
                problemId);
    }

    private long publishedEventCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE topic = ?", Long.class, PUBLISHED_TOPIC);
    }

    private String problemPath() {
        return "/api/studio/problems/" + problemId;
    }
}
