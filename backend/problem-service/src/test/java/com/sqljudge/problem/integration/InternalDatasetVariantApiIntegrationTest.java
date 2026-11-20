package com.sqljudge.problem.integration;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class InternalDatasetVariantApiIntegrationTest extends ContainerSupport {

    private static final String DATASETS = "/api/studio/datasets";
    private static final String INTERNAL_TOKEN = "test-internal-token";
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String SCHEMA =
            """
            {"tables": [{"name": "departments", "columns": [
              {"name": "id", "type": "INTEGER", "isPrimaryKey": true, "isNullable": false},
              {"name": "name", "type": "TEXT", "isPrimaryKey": false, "isNullable": false}]}]}
            """;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteDatasets() {
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Variant công khai của bản đã chốt trả bản, checksum và tên, không kèm dữ liệu")
    void variant_ofFrozenVersion_returnsSnapshotReference() {
        String datasetId = createDataset();
        String variantId = addVariant(datasetId, "Mẫu", true);
        send(HttpMethod.POST, DATASETS + "/" + datasetId + "/freeze", null);

        ResponseEntity<String> response = getVariant(variantId, INTERNAL_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext variant = JsonPath.parse(response.getBody());
        assertThat(variant.read("$.variantId", String.class)).isEqualTo(variantId);
        assertThat(variant.read("$.datasetName", String.class)).isEqualTo("Công ty");
        assertThat(variant.read("$.variantName", String.class)).isEqualTo("Mẫu");
        assertThat(variant.read("$.isPublic", Boolean.class)).isTrue();
        assertThat(variant.read("$.isFrozen", Boolean.class)).isTrue();
        assertThat(variant.read("$.versionId", String.class))
                .isEqualTo(
                        jdbcTemplate.queryForObject(
                                "SELECT BIN_TO_UUID(id) FROM dataset_versions"
                                        + " WHERE dataset_id = UUID_TO_BIN(?)",
                                String.class,
                                datasetId));
        assertThat(variant.read("$.checksum", String.class)).hasSize(64);
        assertThat(response.getBody()).doesNotContain("Kỹ thuật").doesNotContain("csv");
    }

    @Test
    @DisplayName("Variant ẩn và variant của bản nháp vẫn trả về, kèm cờ để bên gọi tự từ chối")
    void variant_hiddenOrDraft_isFlagged() {
        String datasetId = createDataset();
        String hiddenId = addVariant(datasetId, "Ẩn", false);

        DocumentContext draft = JsonPath.parse(getVariant(hiddenId, INTERNAL_TOKEN).getBody());

        assertThat(draft.read("$.isPublic", Boolean.class)).isFalse();
        assertThat(draft.read("$.isFrozen", Boolean.class)).isFalse();
        assertThat(draft.read("$.checksum", String.class)).isNull();
    }

    @Test
    @DisplayName("Variant không tồn tại trả 404, thiếu token nội bộ trả 401")
    void variant_unknownOrWithoutToken_isRejected() {
        String variantId = addVariant(createDataset(), "Mẫu", true);

        ResponseEntity<String> unknown = getVariant(UUID.randomUUID().toString(), INTERNAL_TOKEN);

        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(unknown.getBody()).read("$.code", String.class))
                .isEqualTo("DATASET_VARIANT_NOT_FOUND");
        assertThat(getVariant(variantId, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String createDataset() {
        String id =
                JsonPath.parse(send(HttpMethod.POST, DATASETS, "{\"name\":\"Công ty\"}").getBody())
                        .read("$.id", String.class);
        send(HttpMethod.PUT, DATASETS + "/" + id + "/schema", SCHEMA);
        return id;
    }

    private String addVariant(String datasetId, String name, boolean isPublic) {
        DocumentContext dataset =
                JsonPath.parse(
                        send(
                                        HttpMethod.POST,
                                        DATASETS + "/" + datasetId + "/variants",
                                        "{\"name\":\"" + name + "\",\"isPublic\":" + isPublic + "}")
                                .getBody());
        String variantId =
                dataset.read("$.variants[?(@.name == '" + name + "')].id", java.util.List.class)
                        .get(0)
                        .toString();
        send(
                HttpMethod.PUT,
                DATASETS + "/" + datasetId + "/variants/" + variantId + "/tables/departments",
                "{\"csv\":\"id,name\\n1,Kỹ thuật\\n\"}");
        return variantId;
    }

    private ResponseEntity<String> getVariant(String variantId, String internalToken) {
        HttpHeaders headers = new HttpHeaders();
        if (internalToken != null) {
            headers.set("X-Internal-Token", internalToken);
        }
        return restTemplate.exchange(
                "/internal/dataset-variants/" + variantId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(SETTER);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
