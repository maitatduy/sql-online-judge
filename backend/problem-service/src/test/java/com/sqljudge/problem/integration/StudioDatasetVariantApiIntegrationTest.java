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
class StudioDatasetVariantApiIntegrationTest extends ContainerSupport {

    private static final String PATH = "/api/studio/datasets";
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String MODERATOR = TestJwts.token(UUID.randomUUID(), "MODERATOR");
    private static final String SECRET_ROW = "Phòng bí mật";
    private static final String DEPARTMENTS_CSV = "id,name\\n1,Kỹ thuật\\n2," + SECRET_ROW + "\\n";
    private static final String COMPANY_SCHEMA =
            """
            {"tables": [
              {"name": "departments", "columns": [
                {"name": "id", "type": "INTEGER", "isPrimaryKey": true, "isNullable": false},
                {"name": "name", "type": "TEXT", "isPrimaryKey": false, "isNullable": false}]},
              {"name": "employees", "columns": [
                {"name": "id", "type": "INTEGER", "isPrimaryKey": true, "isNullable": false},
                {"name": "department_id", "type": "INTEGER", "isPrimaryKey": false,
                 "isNullable": true, "references": {"table": "departments", "column": "id"}}]}
            ]}
            """;

    private static final String DEPARTMENTS_ONLY_SCHEMA =
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
    void createDatasetWithVariant() {
        datasetId =
                JsonPath.parse(
                                send(HttpMethod.POST, PATH, SETTER, "{\"name\":\"Công ty\"}")
                                        .getBody())
                        .read("$.id", String.class);
        send(HttpMethod.PUT, url("/schema"), SETTER, COMPANY_SCHEMA);
        variantId =
                JsonPath.parse(
                                send(
                                                HttpMethod.POST,
                                                url("/variants"),
                                                SETTER,
                                                "{\"name\":\"Mẫu\",\"isPublic\":true}")
                                        .getBody())
                        .read("$.variants[0].id", String.class);
    }

    @AfterEach
    void deleteDatasets() {
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Thêm bộ dữ liệu: 201, có trong danh sách bộ dữ liệu của bản nháp")
    void addVariant_validName_returnsDatasetWithVariant() {
        ResponseEntity<String> response =
                send(
                        HttpMethod.POST,
                        url("/variants"),
                        SETTER,
                        "{\"name\":\"Có NULL\",\"isPublic\":false}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DocumentContext dataset = JsonPath.parse(response.getBody());
        assertThat(dataset.read("$.variants[*].name", List.class))
                .containsExactly("Mẫu", "Có NULL");
        assertThat(dataset.read("$.variants[1].isPublic", Boolean.class)).isFalse();
        assertThat(dataset.read("$.variants[1].tables", List.class)).isEmpty();
    }

    @Test
    @DisplayName("Tên bộ dữ liệu trùng trả 409, tên trống trả 400")
    void addVariant_duplicateOrBlankName_isRejected() {
        ResponseEntity<String> duplicate =
                send(HttpMethod.POST, url("/variants"), SETTER, "{\"name\":\"Mẫu\"}");
        ResponseEntity<String> blank =
                send(HttpMethod.POST, url("/variants"), SETTER, "{\"name\":\"\"}");

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(JsonPath.parse(duplicate.getBody()).read("$.code", String.class))
                .isEqualTo("DATASET_VARIANT_NAME_TAKEN");
        assertThat(blank.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Nhập CSV cho một bảng: lưu số dòng, đọc lại được, nhập lại thì thay thế")
    void uploadCsv_validCsv_storesTableData() {
        putCsv("departments", DEPARTMENTS_CSV);
        ResponseEntity<String> replaced = putCsv("departments", "id,name\\n1,Kỹ thuật\\n");

        assertThat(replaced.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext dataset = JsonPath.parse(replaced.getBody());
        assertThat(dataset.read("$.variants[0].tables[*].tableName", List.class))
                .containsExactly("departments");
        assertThat(dataset.read("$.variants[0].tables[0].rowCount", Integer.class)).isEqualTo(1);
        DocumentContext stored =
                JsonPath.parse(send(HttpMethod.GET, csvUrl("departments"), SETTER, null).getBody());
        assertThat(stored.read("$.csv", String.class)).isEqualTo("id,name\n1,Kỹ thuật\n");
        assertThat(stored.read("$.rowCount", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("CSV sai định dạng hoặc bảng không có trong schema trả 400 kèm trường lỗi")
    void uploadCsv_invalidCsvOrUnknownTable_returnsFieldError() {
        ResponseEntity<String> wrongHeader = putCsv("departments", "id,title\\n1,A\\n");
        ResponseEntity<String> unknownTable = putCsv("projects", "id\\n1\\n");

        assertThat(wrongHeader.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(wrongHeader.getBody()).read("$.errors[0].message", String.class))
                .isEqualTo("Dòng tiêu đề phải là: id,name");
        assertThat(unknownTable.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(unknownTable.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("tableName");
    }

    @Test
    @DisplayName("Chi tiết dataset không bao giờ chứa nội dung CSV")
    void getDataset_neverIncludesCsvContent() {
        putCsv("departments", DEPARTMENTS_CSV);

        assertThat(send(HttpMethod.GET, url(""), SETTER, null).getBody())
                .doesNotContain(SECRET_ROW);
    }

    @Test
    @DisplayName("Đổi tên, chuyển sang ẩn, rồi xoá bộ dữ liệu cùng CSV của nó")
    void updateAndDeleteVariant_changesThenRemovesIt() {
        putCsv("departments", DEPARTMENTS_CSV);

        DocumentContext updated =
                JsonPath.parse(
                        send(
                                        HttpMethod.PATCH,
                                        variantUrl(),
                                        SETTER,
                                        "{\"name\":\"Ví dụ\",\"isPublic\":false}")
                                .getBody());
        ResponseEntity<String> deleted = send(HttpMethod.DELETE, variantUrl(), SETTER, null);

        assertThat(updated.read("$.variants[0].name", String.class)).isEqualTo("Ví dụ");
        assertThat(updated.read("$.variants[0].isPublic", Boolean.class)).isFalse();
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(count("dataset_variants")).isZero();
        assertThat(count("dataset_variant_tables")).isZero();
    }

    @Test
    @DisplayName("Bỏ một bảng khỏi schema thì xoá luôn CSV của bảng đó trong mọi bộ dữ liệu")
    void replaceSchema_droppedTable_removesItsCsv() {
        putCsv("departments", DEPARTMENTS_CSV);
        putCsv("employees", "id,department_id\\n1,1\\n");
        DocumentContext dataset =
                JsonPath.parse(
                        send(HttpMethod.PUT, url("/schema"), SETTER, DEPARTMENTS_ONLY_SCHEMA)
                                .getBody());

        assertThat(dataset.read("$.variants[0].tables[*].tableName", List.class))
                .containsExactly("departments");
    }

    @Test
    @DisplayName("MODERATOR đọc được CSV nhưng không sửa; người ra đề khác nhận 404")
    void variantEndpoints_enforcePermissions() {
        putCsv("departments", DEPARTMENTS_CSV);

        assertThat(send(HttpMethod.GET, csvUrl("departments"), MODERATOR, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(
                        send(HttpMethod.PUT, csvUrl("departments"), MODERATOR, csvBody("id,name"))
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.GET, csvUrl("departments"), OTHER_SETTER, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.DELETE, variantUrl(), OTHER_SETTER, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        send(HttpMethod.GET, csvUrl("departments"), TestJwts.learnerToken(), null)
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Phiên bản đã chốt thì không thêm bộ dữ liệu hay nhập CSV được: 422")
    void variantEndpoints_frozenVersion_returnUnprocessable() {
        jdbcTemplate.update("UPDATE dataset_versions SET frozen_at = NOW(6)");

        ResponseEntity<String> addVariant =
                send(HttpMethod.POST, url("/variants"), SETTER, "{\"name\":\"Mới\"}");

        assertThat(addVariant.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(JsonPath.parse(addVariant.getBody()).read("$.code", String.class))
                .isEqualTo("DATASET_VERSION_FROZEN");
        assertThat(putCsv("departments", DEPARTMENTS_CSV).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private ResponseEntity<String> putCsv(String tableName, String csv) {
        return send(HttpMethod.PUT, csvUrl(tableName), SETTER, csvBody(csv));
    }

    private static String csvBody(String csv) {
        return "{\"csv\":\"" + csv + "\"}";
    }

    private String url(String suffix) {
        return PATH + "/" + datasetId + suffix;
    }

    private String variantUrl() {
        return url("/variants/" + variantId);
    }

    private String csvUrl(String tableName) {
        return variantUrl() + "/tables/" + tableName;
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
