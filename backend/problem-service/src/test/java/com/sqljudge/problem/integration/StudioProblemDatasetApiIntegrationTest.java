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
class StudioProblemDatasetApiIntegrationTest extends ContainerSupport {

    private static final String PROBLEMS = "/api/studio/problems";
    private static final String DATASETS = "/api/studio/datasets";
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String SCHEMA =
            """
            {"tables": [
              {"name": "departments", "columns": [
                {"name": "id", "type": "INTEGER", "isPrimaryKey": true, "isNullable": false},
                {"name": "name", "type": "TEXT", "isPrimaryKey": false, "isNullable": false}]}
            ]}
            """;
    private static final String REFERENCED_VERSION =
            "SELECT v.number FROM problems p"
                    + " JOIN problem_revisions r ON r.id = p.draft_revision_id"
                    + " JOIN dataset_versions v ON v.id = r.dataset_version_id"
                    + " WHERE p.id = UUID_TO_BIN(?)";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private String problemId;
    private String datasetId;

    @BeforeEach
    void createProblemAndReadyDataset() {
        problemId = idOf(send(HttpMethod.POST, PROBLEMS, SETTER, "{\"title\":\"Phòng ban\"}"));
        datasetId = createReadyDataset(SETTER, "Công ty");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("UPDATE problems SET draft_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Chọn database cho bài: chốt bản hiện tại, bài trỏ tới bản đã chốt")
    void chooseDataset_readyDataset_freezesAndAttaches() {
        ResponseEntity<String> response = choose(SETTER, datasetId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext problem = JsonPath.parse(response.getBody());
        assertThat(problem.read("$.dataset.id", String.class)).isEqualTo(datasetId);
        assertThat(problem.read("$.dataset.name", String.class)).isEqualTo("Công ty");
        assertThat(versionOfProblem()).isEqualTo(1);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM dataset_versions WHERE frozen_at IS NOT NULL",
                                Long.class))
                .isEqualTo(1);
        assertThat(
                        JsonPath.parse(send(HttpMethod.GET, problemUrl(), SETTER, null).getBody())
                                .read("$.dataset.name", String.class))
                .isEqualTo("Công ty");
    }

    @Test
    @DisplayName("Sửa database sau khi chọn thì bài vẫn dùng bản cũ, chọn lại thì dùng bản mới")
    void chooseDataset_afterEditingDataset_keepsOldUntilChosenAgain() {
        choose(SETTER, datasetId);
        send(HttpMethod.PUT, DATASETS + "/" + datasetId + "/schema", SETTER, SCHEMA);

        assertThat(versionOfProblem()).isEqualTo(1);
        choose(SETTER, datasetId);

        assertThat(versionOfProblem()).isEqualTo(2);
    }

    @Test
    @DisplayName("Database chưa đủ dữ liệu trả 422 DATASET_NOT_READY kèm lý do")
    void chooseDataset_incompleteDataset_returnsReason() {
        String emptyDataset = idOf(send(HttpMethod.POST, DATASETS, SETTER, "{\"name\":\"Trống\"}"));

        ResponseEntity<String> response = choose(SETTER, emptyDataset);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        DocumentContext error = JsonPath.parse(response.getBody());
        assertThat(error.read("$.code", String.class)).isEqualTo("DATASET_NOT_READY");
        assertThat(error.read("$.message", String.class))
                .isEqualTo("Cần ít nhất một bảng trong schema");
    }

    @Test
    @DisplayName("Không chọn được database của người khác: 404 DATASET_NOT_FOUND")
    void chooseDataset_otherSettersDataset_returnsNotFound() {
        String othersDataset = createReadyDataset(OTHER_SETTER, "Trường học");

        ResponseEntity<String> response = choose(SETTER, othersDataset);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("DATASET_NOT_FOUND");
    }

    @Test
    @DisplayName("Bài đã gửi duyệt hoặc xuất bản thì không đổi database: 422 PROBLEM_LOCKED")
    void chooseDataset_lockedProblem_returnsLocked() {
        jdbcTemplate.update("UPDATE problems SET status = 'PENDING_REVIEW'");

        ResponseEntity<String> response = choose(SETTER, datasetId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("PROBLEM_LOCKED");
    }

    @Test
    @DisplayName("Thiếu datasetId trả 400, học viên nhận 403, người ra đề khác nhận 404")
    void chooseDataset_invalidRequestOrRole_isRejected() {
        ResponseEntity<String> missing =
                send(HttpMethod.PUT, problemUrl() + "/dataset", SETTER, "{}");

        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(missing.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("datasetId");
        assertThat(choose(TestJwts.learnerToken(), datasetId).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(choose(OTHER_SETTER, datasetId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String createReadyDataset(String token, String name) {
        String id = idOf(send(HttpMethod.POST, DATASETS, token, "{\"name\":\"" + name + "\"}"));
        send(HttpMethod.PUT, DATASETS + "/" + id + "/schema", token, SCHEMA);
        String variantId =
                JsonPath.parse(
                                send(
                                                HttpMethod.POST,
                                                DATASETS + "/" + id + "/variants",
                                                token,
                                                "{\"name\":\"Mẫu\",\"isPublic\":true}")
                                        .getBody())
                        .read("$.variants[0].id", String.class);
        send(
                HttpMethod.PUT,
                DATASETS + "/" + id + "/variants/" + variantId + "/tables/departments",
                token,
                "{\"csv\":\"id,name\\n1,Kỹ thuật\\n\"}");
        return id;
    }

    private ResponseEntity<String> choose(String token, String chosenDatasetId) {
        return send(
                HttpMethod.PUT,
                problemUrl() + "/dataset",
                token,
                "{\"datasetId\":\"" + chosenDatasetId + "\"}");
    }

    private Integer versionOfProblem() {
        return jdbcTemplate.queryForObject(REFERENCED_VERSION, Integer.class, problemId);
    }

    private String problemUrl() {
        return PROBLEMS + "/" + problemId;
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
