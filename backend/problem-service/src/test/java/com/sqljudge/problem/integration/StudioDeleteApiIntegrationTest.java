package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class StudioDeleteApiIntegrationTest extends ContainerSupport {

    private static final String PROBLEMS = "/api/studio/problems";
    private static final String DATASETS = "/api/studio/datasets";
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String MODERATOR = TestJwts.token(UUID.randomUUID(), "MODERATOR");

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private String problemId;
    private String datasetId;

    @BeforeEach
    void createProblemAndDataset() {
        problemId = idOf(send(HttpMethod.POST, PROBLEMS, SETTER, "{\"title\":\"Bài thử\"}"));
        datasetId = idOf(send(HttpMethod.POST, DATASETS, SETTER, "{\"name\":\"Công ty\"}"));
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("UPDATE problems SET draft_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Tác giả xoá bài nháp: 204, bài biến mất khỏi Studio nhưng vẫn còn trong DB")
    void deleteProblem_ownDraft_softDeletes() {
        ResponseEntity<String> response = send(HttpMethod.DELETE, problemUrl(), SETTER, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(send(HttpMethod.GET, problemUrl(), SETTER, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        JsonPath.parse(send(HttpMethod.GET, PROBLEMS, SETTER, null).getBody())
                                .read("$.totalItems", Integer.class))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM problems WHERE deleted_at IS NOT NULL"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Người ra đề không xoá được bài đã xuất bản: 422 PROBLEM_LOCKED")
    void deleteProblem_publishedBySetter_isLocked() {
        jdbcTemplate.update("UPDATE problems SET status = 'PUBLISHED'");

        ResponseEntity<String> response = send(HttpMethod.DELETE, problemUrl(), SETTER, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("PROBLEM_LOCKED");
    }

    @Test
    @DisplayName("ADMIN xoá mềm được cả bài đã xuất bản")
    void deleteProblem_publishedByAdmin_softDeletes() {
        jdbcTemplate.update("UPDATE problems SET status = 'PUBLISHED'");

        assertThat(send(HttpMethod.DELETE, problemUrl(), ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(count("SELECT COUNT(*) FROM problems WHERE deleted_at IS NOT NULL"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Người ra đề khác nhận 404, học viên nhận 403 khi xoá bài")
    void deleteProblem_otherSetterOrLearner_isRejected() {
        assertThat(send(HttpMethod.DELETE, problemUrl(), OTHER_SETTER, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        send(HttpMethod.DELETE, problemUrl(), TestJwts.learnerToken(), null)
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(count("SELECT COUNT(*) FROM problems WHERE deleted_at IS NULL")).isEqualTo(1);
    }

    @Test
    @DisplayName("Xoá dataset chưa bài nào dùng: 204, dataset biến mất khỏi Studio")
    void deleteDataset_unused_softDeletes() {
        ResponseEntity<String> response = send(HttpMethod.DELETE, datasetUrl(), SETTER, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(send(HttpMethod.GET, datasetUrl(), SETTER, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        JsonPath.parse(send(HttpMethod.GET, DATASETS, SETTER, null).getBody())
                                .read("$.totalItems", Integer.class))
                .isZero();
    }

    @Test
    @DisplayName("Dataset đang được bài dùng thì không xoá được: 409 DATASET_IN_USE kèm số bài")
    void deleteDataset_usedByProblem_isRejected() {
        useDatasetInProblem();

        ResponseEntity<String> response = send(HttpMethod.DELETE, datasetUrl(), SETTER, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("DATASET_IN_USE");
        assertThat(JsonPath.parse(response.getBody()).read("$.message", String.class))
                .isEqualTo("Dataset đang được 1 bài dùng nên chưa xoá được");
    }

    @Test
    @DisplayName("Dataset chỉ còn bài đã xoá dùng thì xoá được")
    void deleteDataset_usedOnlyByDeletedProblem_softDeletes() {
        useDatasetInProblem();
        send(HttpMethod.DELETE, problemUrl(), SETTER, null);

        assertThat(send(HttpMethod.DELETE, datasetUrl(), SETTER, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Người ra đề khác nhận 404, MODERATOR nhận 403 khi xoá dataset")
    void deleteDataset_otherSetterOrModerator_isRejected() {
        assertThat(send(HttpMethod.DELETE, datasetUrl(), OTHER_SETTER, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.DELETE, datasetUrl(), MODERATOR, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void useDatasetInProblem() {
        jdbcTemplate.update(
                "UPDATE problem_revisions SET dataset_version_id ="
                        + " (SELECT id FROM dataset_versions WHERE dataset_id = UUID_TO_BIN(?))",
                datasetId);
    }

    private String problemUrl() {
        return PROBLEMS + "/" + problemId;
    }

    private String datasetUrl() {
        return DATASETS + "/" + datasetId;
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    private static String idOf(ResponseEntity<String> response) {
        return JsonPath.parse(response.getBody()).read("$.id", String.class);
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
