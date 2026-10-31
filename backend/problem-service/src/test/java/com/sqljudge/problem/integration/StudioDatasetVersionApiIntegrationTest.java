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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class StudioDatasetVersionApiIntegrationTest extends ContainerSupport {

    private static final String PATH = "/api/studio/datasets";
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String MODERATOR = TestJwts.token(UUID.randomUUID(), "MODERATOR");
    private static final String SCHEMA =
            """
            {"tables": [
              {"name": "departments", "columns": [
                {"name": "id", "type": "INTEGER", "isPrimaryKey": true, "isNullable": false},
                {"name": "name", "type": "TEXT", "isPrimaryKey": false, "isNullable": false}]}
            ]}
            """;

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private String datasetId;
    private String variantId;

    @BeforeEach
    void createReadyDraft() {
        datasetId =
                JsonPath.parse(
                                send(HttpMethod.POST, PATH, SETTER, "{\"name\":\"Công ty\"}")
                                        .getBody())
                        .read("$.id", String.class);
        send(HttpMethod.PUT, url("/schema"), SETTER, SCHEMA);
        variantId =
                JsonPath.parse(
                                send(
                                                HttpMethod.POST,
                                                url("/variants"),
                                                SETTER,
                                                "{\"name\":\"Mẫu\",\"isPublic\":true}")
                                        .getBody())
                        .read("$.variants[0].id", String.class);
        send(
                HttpMethod.PUT,
                url("/variants/" + variantId + "/tables/departments"),
                SETTER,
                "{\"csv\":\"id,name\\n1,Kỹ thuật\\n2,Kinh doanh\\n\"}");
    }

    @AfterEach
    void deleteDatasets() {
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Chốt bản nháp đủ điều kiện: ghi thời điểm chốt và checksum, không sửa được nữa")
    void freeze_readyDraft_freezesVersion() {
        ResponseEntity<String> response = send(HttpMethod.POST, url("/freeze"), SETTER, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext dataset = JsonPath.parse(response.getBody());
        assertThat(dataset.read("$.versions[0].frozenAt", String.class)).isNotNull();
        assertThat(dataset.read("$.schema.isDraft", Boolean.class)).isFalse();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT checksum FROM dataset_versions", String.class))
                .hasSize(64);
        assertThat(send(HttpMethod.PUT, url("/schema"), SETTER, SCHEMA).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("Bản nháp chưa đủ điều kiện trả 422 DATASET_NOT_READY kèm lý do dễ hiểu")
    void freeze_incompleteDraft_returnsReason() {
        send(HttpMethod.POST, url("/variants"), SETTER, "{\"name\":\"Có NULL\"}");

        ResponseEntity<String> response = send(HttpMethod.POST, url("/freeze"), SETTER, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        DocumentContext error = JsonPath.parse(response.getBody());
        assertThat(error.read("$.code", String.class)).isEqualTo("DATASET_NOT_READY");
        assertThat(error.read("$.message", String.class))
                .isEqualTo("Bộ dữ liệu Có NULL chưa có CSV cho bảng departments");
    }

    @Test
    @DisplayName("Chốt lần hai trả 422 DATASET_VERSION_FROZEN")
    void freeze_alreadyFrozen_returnsUnprocessable() {
        send(HttpMethod.POST, url("/freeze"), SETTER, null);

        ResponseEntity<String> response = send(HttpMethod.POST, url("/freeze"), SETTER, null);

        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("DATASET_VERSION_FROZEN");
    }

    @Test
    @DisplayName("Tạo bản nháp mới sao chép đủ schema, bộ dữ liệu và CSV của phiên bản đã chốt")
    void createVersion_afterFreeze_copiesLatestVersion() {
        send(HttpMethod.POST, url("/freeze"), SETTER, null);

        ResponseEntity<String> response = send(HttpMethod.POST, url("/versions"), SETTER, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DocumentContext dataset = JsonPath.parse(response.getBody());
        assertThat(dataset.read("$.versions[*].number", List.class)).containsExactly(1, 2);
        assertThat(dataset.read("$.schema.versionNumber", Integer.class)).isEqualTo(2);
        assertThat(dataset.read("$.schema.isDraft", Boolean.class)).isTrue();
        assertThat(dataset.read("$.schema.tables[0].columns[*].name", List.class))
                .containsExactly("id", "name");
        assertThat(dataset.read("$.variants[0].name", String.class)).isEqualTo("Mẫu");
        assertThat(dataset.read("$.variants[0].tables[0].rowCount", Integer.class)).isEqualTo(2);
        assertThat(count("dataset_variant_tables")).isEqualTo(2);
        assertThat(send(HttpMethod.PUT, url("/schema"), SETTER, SCHEMA).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Đã có bản nháp thì không tạo thêm được: 422 DATASET_DRAFT_EXISTS")
    void createVersion_draftExists_returnsUnprocessable() {
        ResponseEntity<String> response = send(HttpMethod.POST, url("/versions"), SETTER, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("DATASET_DRAFT_EXISTS");
    }

    @Test
    @DisplayName("Người ra đề khác nhận 404, MODERATOR không chốt hay tạo phiên bản được")
    void versionEndpoints_enforcePermissions() {
        assertThat(send(HttpMethod.POST, url("/freeze"), OTHER_SETTER, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.POST, url("/freeze"), MODERATOR, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.POST, url("/versions"), MODERATOR, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String url(String suffix) {
        return PATH + "/" + datasetId + suffix;
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
