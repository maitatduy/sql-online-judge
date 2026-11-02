package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import java.util.List;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class InternalDatasetSnapshotApiIntegrationTest extends ContainerSupport {

    private static final String DATASETS = "/api/studio/datasets";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String INTERNAL_TOKEN = "test-internal-token";
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String SCHEMA =
            """
            {"tables": [
              {"name": "departments", "columns": [
                {"name": "id", "type": "INTEGER", "isPrimaryKey": true, "isNullable": false},
                {"name": "name", "type": "TEXT", "isPrimaryKey": false, "isNullable": false},
                {"name": "note", "type": "TEXT", "isPrimaryKey": false, "isNullable": true}]},
              {"name": "employees", "columns": [
                {"name": "id", "type": "INTEGER", "isPrimaryKey": true, "isNullable": false},
                {"name": "department_id", "type": "INTEGER", "isPrimaryKey": false,
                 "isNullable": true, "references": {"table": "departments", "column": "id"}}]}
            ]}
            """;

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteDatasets() {
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Token nội bộ đúng thì trả snapshot của bản đã chốt: schema và các dòng đã parse")
    void snapshot_frozenVersion_returnsSchemaAndRows() {
        String versionId = frozenVersionOf(createReadyDataset());

        ResponseEntity<String> response = getSnapshot(versionId, INTERNAL_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext snapshot = JsonPath.parse(response.getBody());
        assertThat(snapshot.read("$.versionId", String.class)).isEqualTo(versionId);
        assertThat(snapshot.read("$.checksum", String.class))
                .isEqualTo(
                        jdbcTemplate.queryForObject(
                                "SELECT checksum FROM dataset_versions WHERE id = UUID_TO_BIN(?)",
                                String.class,
                                versionId));
        assertThat(snapshot.read("$.tables[*].name", List.class))
                .containsExactly("departments", "employees");
        assertThat(snapshot.read("$.tables[0].columns[2].isNullable", Boolean.class)).isTrue();
        assertThat(snapshot.read("$.tables[1].columns[1].references.table", String.class))
                .isEqualTo("departments");
        assertThat(snapshot.read("$.variants[0].name", String.class)).isEqualTo("Mẫu");
        assertThat(snapshot.read("$.variants[0].isPublic", Boolean.class)).isTrue();
        assertThat(snapshot.read("$.variants[0].tables[0].tableName", String.class))
                .isEqualTo("departments");
        assertThat(snapshot.read("$.variants[0].tables[0].rows", List.class))
                .containsExactly(
                        Arrays.asList("1", "Kỹ thuật", null), List.of("2", "Kinh doanh", "Tầng 3"));
    }

    @Test
    @DisplayName("Thiếu token, sai token hay chỉ có JWT người dùng đều bị từ chối 401")
    void snapshot_withoutValidInternalToken_isUnauthorized() {
        String versionId = frozenVersionOf(createReadyDataset());

        assertThat(getSnapshot(versionId, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getSnapshot(versionId, "wrong-token").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        HttpHeaders bearerOnly = new HttpHeaders();
        bearerOnly.setBearerAuth(TestJwts.token(UUID.randomUUID(), "ADMIN"));
        ResponseEntity<String> withJwt =
                restTemplate.exchange(
                        snapshotPath(versionId),
                        HttpMethod.GET,
                        new HttpEntity<>(bearerOnly),
                        String.class);
        assertThat(withJwt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(JsonPath.parse(withJwt.getBody()).read("$.code", String.class))
                .isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("Bản nháp hoặc bản không tồn tại trả 404")
    void snapshot_draftOrUnknownVersion_isNotFound() {
        String datasetId = createReadyDataset();
        String draftVersionId =
                jdbcTemplate.queryForObject(
                        "SELECT BIN_TO_UUID(id) FROM dataset_versions"
                                + " WHERE dataset_id = UUID_TO_BIN(?)",
                        String.class,
                        datasetId);

        ResponseEntity<String> draft = getSnapshot(draftVersionId, INTERNAL_TOKEN);
        ResponseEntity<String> unknown = getSnapshot(UUID.randomUUID().toString(), INTERNAL_TOKEN);

        assertThat(draft.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(draft.getBody()).read("$.code", String.class))
                .isEqualTo("DATASET_NOT_FOUND");
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String createReadyDataset() {
        String id =
                JsonPath.parse(send(HttpMethod.POST, DATASETS, "{\"name\":\"Công ty\"}").getBody())
                        .read("$.id", String.class);
        send(HttpMethod.PUT, DATASETS + "/" + id + "/schema", SCHEMA);
        String variantId =
                JsonPath.parse(
                                send(
                                                HttpMethod.POST,
                                                DATASETS + "/" + id + "/variants",
                                                "{\"name\":\"Mẫu\",\"isPublic\":true}")
                                        .getBody())
                        .read("$.variants[0].id", String.class);
        String tables = DATASETS + "/" + id + "/variants/" + variantId + "/tables/";
        send(
                HttpMethod.PUT,
                tables + "departments",
                "{\"csv\":\"id,name,note\\n1,Kỹ thuật,\\n2,Kinh doanh,Tầng 3\\n\"}");
        send(HttpMethod.PUT, tables + "employees", "{\"csv\":\"id,department_id\\n1,1\\n2,\\n\"}");
        return id;
    }

    private String frozenVersionOf(String datasetId) {
        send(HttpMethod.POST, DATASETS + "/" + datasetId + "/freeze", null);
        return jdbcTemplate.queryForObject(
                "SELECT BIN_TO_UUID(id) FROM dataset_versions"
                        + " WHERE dataset_id = UUID_TO_BIN(?) AND frozen_at IS NOT NULL",
                String.class,
                datasetId);
    }

    private ResponseEntity<String> getSnapshot(String versionId, String internalToken) {
        HttpHeaders headers = new HttpHeaders();
        if (internalToken != null) {
            headers.set(INTERNAL_TOKEN_HEADER, internalToken);
        }
        return restTemplate.exchange(
                snapshotPath(versionId), HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private static String snapshotPath(String versionId) {
        return "/internal/dataset-versions/" + versionId + "/snapshot";
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SETTER);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
