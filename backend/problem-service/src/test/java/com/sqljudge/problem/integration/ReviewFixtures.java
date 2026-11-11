package com.sqljudge.problem.integration;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;

final class ReviewFixtures {

    private ReviewFixtures() {}

    static String submitForReview(
            StudioFixtures studio, JdbcTemplate jdbcTemplate, String expectedRowsJson) {
        String problemId = studio.createProblem("Phòng ban");
        String datasetId = studio.createReadyDataset("Công ty");
        String hiddenVariantId = studio.addVariant(datasetId, "Ẩn", false);
        studio.chooseDataset(problemId, datasetId);
        studio.saveAnswer(problemId, studio.variantIdsOf(datasetId).get(0), hiddenVariantId);
        String problemPath = "/api/studio/problems/" + problemId;
        studio.send(
                HttpMethod.PATCH,
                problemPath,
                "{\"statementMarkdown\":\"Liệt kê tên phòng ban.\"}");
        String checkId =
                JsonPath.parse(
                                studio.send(HttpMethod.POST, problemPath + "/answer-checks", null)
                                        .getBody())
                        .read("$.id", String.class);
        jdbcTemplate.update(
                "UPDATE answer_checks SET status = 'PASSED', completed_at = NOW(6)"
                        + " WHERE id = UUID_TO_BIN(?)",
                checkId);
        jdbcTemplate.update(
                "INSERT INTO expected_results (id, testcase_id, columns_json, rows_json,"
                        + " row_count, checksum, created_at, updated_at)"
                        + " SELECT UUID_TO_BIN(UUID()), t.id, '[\"name\"]', ?, 1,"
                        + " REPEAT('0', 64), NOW(6), NOW(6)"
                        + " FROM testcases t JOIN problems p ON p.draft_revision_id = t.revision_id"
                        + " WHERE p.id = UUID_TO_BIN(?)",
                expectedRowsJson,
                problemId);
        studio.send(HttpMethod.POST, problemPath + "/submit-for-review", null);
        return problemId;
    }
}
