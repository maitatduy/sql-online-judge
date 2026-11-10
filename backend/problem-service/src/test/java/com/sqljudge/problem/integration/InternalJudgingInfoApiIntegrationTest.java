package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class InternalJudgingInfoApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String INTERNAL_TOKEN = "test-internal-token";

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
                studioPath(),
                "{\"statementMarkdown\":\"Liệt kê tên phòng ban.\"}");
        String checkId =
                JsonPath.parse(
                                studio.send(HttpMethod.POST, studioPath() + "/answer-checks", null)
                                        .getBody())
                        .read("$.id", String.class);
        jdbcTemplate.update(
                "UPDATE answer_checks SET status = 'PASSED', completed_at = NOW(6)"
                        + " WHERE id = UUID_TO_BIN(?)",
                checkId);
        jdbcTemplate.update(
                "INSERT INTO expected_results (id, testcase_id, columns_json, rows_json,"
                        + " row_count, checksum, created_at, updated_at)"
                        + " SELECT UUID_TO_BIN(UUID()), t.id, '[\"name\"]', '[]', 0,"
                        + " REPEAT('0', 64), NOW(6), NOW(6)"
                        + " FROM testcases t JOIN problems p ON p.draft_revision_id = t.revision_id"
                        + " WHERE p.id = UUID_TO_BIN(?)",
                problemId);
        studio.send(HttpMethod.POST, studioPath() + "/submit-for-review", null);
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
    @DisplayName("Bài đã xuất bản trả bản đã chốt, các dialect, số testcase và giới hạn thời gian")
    void judgingInfo_publishedProblem_returnsPublishedRevision() {
        studio.send(HttpMethod.POST, "/api/admin/problems/" + problemId + "/approve", null, ADMIN);

        ResponseEntity<String> response = judgingInfo(problemId, INTERNAL_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext info = JsonPath.parse(response.getBody());
        assertThat(info.read("$.problemId", String.class)).isEqualTo(problemId);
        assertThat(info.read("$.revisionId", String.class))
                .isEqualTo(
                        jdbcTemplate.queryForObject(
                                "SELECT BIN_TO_UUID(published_revision_id) FROM problems"
                                        + " WHERE id = UUID_TO_BIN(?)",
                                String.class,
                                problemId));
        assertThat(info.read("$.dialects", List.class)).containsExactly("POSTGRESQL", "SQLITE");
        assertThat(info.read("$.testcaseCount", Integer.class)).isEqualTo(2);
        assertThat(info.read("$.timeLimitMs", Integer.class)).isEqualTo(2000);
    }

    @Test
    @DisplayName("Bài chưa xuất bản hoặc không tồn tại thì trả 404")
    void judgingInfo_unpublishedOrUnknown_isNotFound() {
        ResponseEntity<String> pending = judgingInfo(problemId, INTERNAL_TOKEN);
        assertThat(pending.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(pending.getBody()).read("$.code", String.class))
                .isEqualTo("PROBLEM_NOT_FOUND");

        assertThat(judgingInfo(UUID.randomUUID().toString(), INTERNAL_TOKEN).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Thiếu hoặc sai token nội bộ thì trả 401, kể cả khi có JWT người dùng")
    void judgingInfo_withoutInternalToken_isUnauthorized() {
        studio.send(HttpMethod.POST, "/api/admin/problems/" + problemId + "/approve", null, ADMIN);

        assertThat(judgingInfo(problemId, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(judgingInfo(problemId, "wrong-token").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(
                        studio.send(
                                        HttpMethod.GET,
                                        "/internal/problems/" + problemId + "/judging-info",
                                        null,
                                        ADMIN)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<String> judgingInfo(String id, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set(INTERNAL_TOKEN_HEADER, token);
        }
        return restTemplate.exchange(
                "/internal/problems/" + id + "/judging-info",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }

    private String studioPath() {
        return "/api/studio/problems/" + problemId;
    }
}
