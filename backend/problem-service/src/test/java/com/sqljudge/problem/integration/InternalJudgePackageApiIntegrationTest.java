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
class InternalJudgePackageApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String INTERNAL_TOKEN = "test-internal-token";
    private static final String EXPECTED_ROWS = "[[{\"kind\":\"TEXT\",\"value\":\"Kỹ thuật\"}]]";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private StudioFixtures studio;
    private String problemId;

    @BeforeEach
    void submitProblemForReview() {
        studio = new StudioFixtures(restTemplate, SETTER);
        problemId = ReviewFixtures.submitForReview(studio, jdbcTemplate, EXPECTED_ROWS);
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
    @DisplayName(
            "Bản đã xuất bản trả gói chấm: cài đặt so khớp, testcase theo thứ tự và kết quả mong"
                    + " đợi")
    void judgePackage_publishedRevision_returnsTestcasesAndExpectedResults() {
        studio.send(HttpMethod.POST, "/api/admin/problems/" + problemId + "/approve", null, ADMIN);
        String revisionId = publishedRevisionId();

        ResponseEntity<String> response = judgePackage(revisionId, INTERNAL_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext judgePackage = JsonPath.parse(response.getBody());
        assertThat(judgePackage.read("$.revisionId", String.class)).isEqualTo(revisionId);
        assertThat(judgePackage.read("$.datasetChecksum", String.class))
                .isEqualTo(
                        jdbcTemplate.queryForObject(
                                "SELECT v.checksum FROM problem_revisions r"
                                        + " JOIN dataset_versions v ON v.id = r.dataset_version_id"
                                        + " WHERE r.id = UUID_TO_BIN(?)",
                                String.class,
                                revisionId));
        assertThat(judgePackage.read("$.datasetVersionId", String.class)).isNotBlank();
        assertThat(judgePackage.read("$.requiresOrder", Boolean.class)).isTrue();
        assertThat(judgePackage.read("$.compareColumnNames", Boolean.class)).isFalse();
        assertThat(judgePackage.read("$.floatTolerance", Double.class)).isEqualTo(0.01);
        assertThat(judgePackage.read("$.timeLimitMs", Integer.class)).isEqualTo(2000);
        assertThat(judgePackage.read("$.allowedStatements", List.class))
                .containsExactlyInAnyOrder("SELECT", "WITH");
        assertThat(judgePackage.read("$.testcases[*].index", List.class)).containsExactly(1, 2);
        assertThat(judgePackage.read("$.testcases[*].isPublic", List.class))
                .containsExactly(true, false);
        assertThat(judgePackage.read("$.testcases[0].variantId", String.class)).isNotBlank();
        assertThat(judgePackage.read("$.testcases[1].expected.columns", List.class))
                .containsExactly("name");
        assertThat(judgePackage.read("$.testcases[1].expected.rows[0][0].value", String.class))
                .isEqualTo("Kỹ thuật");
    }

    @Test
    @DisplayName("Bản chưa chốt hoặc không tồn tại trả 404; thiếu token nội bộ trả 401")
    void judgePackage_unfrozenUnknownOrWithoutToken_isRejected() {
        String draftRevisionId =
                jdbcTemplate.queryForObject(
                        "SELECT BIN_TO_UUID(draft_revision_id) FROM problems"
                                + " WHERE id = UUID_TO_BIN(?)",
                        String.class,
                        problemId);

        ResponseEntity<String> draft = judgePackage(draftRevisionId, INTERNAL_TOKEN);
        assertThat(draft.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(draft.getBody()).read("$.code", String.class))
                .isEqualTo("PROBLEM_REVISION_NOT_FOUND");
        assertThat(judgePackage(UUID.randomUUID().toString(), INTERNAL_TOKEN).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(judgePackage(draftRevisionId, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String publishedRevisionId() {
        return jdbcTemplate.queryForObject(
                "SELECT BIN_TO_UUID(published_revision_id) FROM problems"
                        + " WHERE id = UUID_TO_BIN(?)",
                String.class,
                problemId);
    }

    private ResponseEntity<String> judgePackage(String revisionId, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set("X-Internal-Token", token);
        }
        return restTemplate.exchange(
                "/internal/problem-revisions/" + revisionId + "/judge-package",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }
}
