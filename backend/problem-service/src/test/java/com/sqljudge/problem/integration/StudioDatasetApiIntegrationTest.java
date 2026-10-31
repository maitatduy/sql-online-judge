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
class StudioDatasetApiIntegrationTest extends ContainerSupport {

    private static final String PATH = "/api/studio/datasets";
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String MODERATOR = TestJwts.token(UUID.randomUUID(), "MODERATOR");
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

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private String datasetId;

    @BeforeEach
    void createDataset() {
        ResponseEntity<String> created =
                send(
                        HttpMethod.POST,
                        PATH,
                        SETTER,
                        "{\"name\":\"Công ty\",\"description\":\"Phòng ban và nhân viên\"}");
        datasetId = JsonPath.parse(created.getBody()).read("$.id", String.class);
    }

    @AfterEach
    void deleteDatasets() {
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Tạo dataset: 201 kèm Location, slug sinh từ tên, có sẵn phiên bản 1 dạng nháp")
    void create_validName_returnsDraftDataset() {
        ResponseEntity<String> response =
                send(HttpMethod.POST, PATH, SETTER, "{\"name\":\"Công ty\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DocumentContext dataset = JsonPath.parse(response.getBody());
        String id = dataset.read("$.id", String.class);
        assertThat(response.getHeaders().getLocation()).hasToString(PATH + "/" + id);
        assertThat(dataset.read("$.slug", String.class)).isEqualTo("cong-ty-2");
        assertThat(dataset.read("$.description", String.class)).isEmpty();
        assertThat(dataset.read("$.versions[*].number", List.class)).containsExactly(1);
        assertThat(dataset.read("$.schema.versionNumber", Integer.class)).isEqualTo(1);
        assertThat(dataset.read("$.schema.isDraft", Boolean.class)).isTrue();
        assertThat(dataset.read("$.schema.tables", List.class)).isEmpty();
    }

    @Test
    @DisplayName("Tạo dataset với tên trống trả 400 kèm lỗi ở trường name")
    void create_blankName_returnsValidationError() {
        ResponseEntity<String> response = send(HttpMethod.POST, PATH, SETTER, "{\"name\":\" \"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(response.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("name");
    }

    @Test
    @DisplayName("Học viên không tạo được dataset, chưa đăng nhập thì 401")
    void create_learnerOrAnonymous_isRejected() {
        assertThat(
                        send(HttpMethod.POST, PATH, TestJwts.learnerToken(), "{\"name\":\"X\"}")
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(
                        restTemplate
                                .postForEntity(PATH, json("{\"name\":\"X\"}"), String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Dataset của tôi chỉ gồm dataset của người gọi, lọc được theo tên")
    void list_returnsOnlyOwnDatasets() {
        send(HttpMethod.POST, PATH, SETTER, "{\"name\":\"Trường học\"}");
        send(HttpMethod.POST, PATH, OTHER_SETTER, "{\"name\":\"Cửa hàng\"}");

        DocumentContext page = JsonPath.parse(send(HttpMethod.GET, PATH, SETTER, null).getBody());
        DocumentContext filtered =
                JsonPath.parse(send(HttpMethod.GET, PATH + "?q=truong", SETTER, null).getBody());

        assertThat(page.read("$.totalItems", Integer.class)).isEqualTo(2);
        assertThat(page.read("$.items[*].name", List.class))
                .containsExactlyInAnyOrder("Công ty", "Trường học");
        assertThat(page.read("$.items[0].latestVersionNumber", Integer.class)).isEqualTo(1);
        assertThat(page.read("$.items[0].hasDraft", Boolean.class)).isTrue();
        assertThat(filtered.read("$.items[*].name", List.class)).containsExactly("Trường học");
    }

    @Test
    @DisplayName("Chủ dataset, ADMIN và MODERATOR đọc được chi tiết, người ra đề khác nhận 404")
    void get_ownerAdminOrModerator_returnsDataset() {
        assertThat(send(HttpMethod.GET, url(), SETTER, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(send(HttpMethod.GET, url(), ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(send(HttpMethod.GET, url(), MODERATOR, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        ResponseEntity<String> other = send(HttpMethod.GET, url(), OTHER_SETTER, null);
        assertThat(other.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(other.getBody()).read("$.code", String.class))
                .isEqualTo("DATASET_NOT_FOUND");
    }

    @Test
    @DisplayName("Sửa tên và mô tả, trường không gửi lên giữ nguyên")
    void update_changesOnlySentFields() {
        DocumentContext updated =
                JsonPath.parse(
                        send(HttpMethod.PATCH, url(), SETTER, "{\"name\":\"Tập đoàn\"}").getBody());

        assertThat(updated.read("$.name", String.class)).isEqualTo("Tập đoàn");
        assertThat(updated.read("$.description", String.class)).isEqualTo("Phòng ban và nhân viên");
        assertThat(send(HttpMethod.PATCH, url(), OTHER_SETTER, "{\"name\":\"X\"}").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Thay schema của bản nháp, lưu lại lần nữa với cùng tên bảng vẫn được")
    void replaceSchema_validSchema_replacesDraftTables() {
        send(HttpMethod.PUT, schemaUrl(), SETTER, COMPANY_SCHEMA);
        ResponseEntity<String> response = send(HttpMethod.PUT, schemaUrl(), SETTER, COMPANY_SCHEMA);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext dataset = JsonPath.parse(response.getBody());
        assertThat(dataset.read("$.schema.tables[*].name", List.class))
                .containsExactly("departments", "employees");
        assertThat(dataset.read("$.schema.tables[1].columns[1].references.table", String.class))
                .isEqualTo("departments");
        assertThat(dataset.read("$.schema.tables[0].columns[0].isPrimaryKey", Boolean.class))
                .isTrue();
        assertThat(count("dataset_tables")).isEqualTo(2);
        assertThat(count("dataset_columns")).isEqualTo(4);
    }

    @Test
    @DisplayName("Tên bảng có dấu hoặc trùng từ khoá SQL trả 400 kèm đường dẫn tới trường lỗi")
    void replaceSchema_invalidNames_returnsFieldErrors() {
        ResponseEntity<String> accented =
                send(
                        HttpMethod.PUT,
                        schemaUrl(),
                        SETTER,
                        COMPANY_SCHEMA.replace(
                                "\"departments\", \"columns\"", "\"Phòng ban\", \"columns\""));
        ResponseEntity<String> reserved =
                send(
                        HttpMethod.PUT,
                        schemaUrl(),
                        SETTER,
                        COMPANY_SCHEMA.replace("\"name\": \"name\"", "\"name\": \"order\""));

        assertThat(accented.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(accented.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("tables[0].name");
        assertThat(reserved.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(reserved.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("tables[0].columns[1].name");
    }

    @Test
    @DisplayName("Phiên bản mới nhất đã chốt thì không sửa schema được: 422 DATASET_VERSION_FROZEN")
    void replaceSchema_frozenVersion_returnsUnprocessable() {
        jdbcTemplate.update("UPDATE dataset_versions SET frozen_at = NOW(6)");

        ResponseEntity<String> response = send(HttpMethod.PUT, schemaUrl(), SETTER, COMPANY_SCHEMA);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("DATASET_VERSION_FROZEN");
    }

    @Test
    @DisplayName("MODERATOR chỉ xem, không sửa schema được; người ra đề khác nhận 404")
    void replaceSchema_moderatorOrOtherSetter_isRejected() {
        assertThat(send(HttpMethod.PUT, schemaUrl(), MODERATOR, COMPANY_SCHEMA).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.PUT, schemaUrl(), OTHER_SETTER, COMPANY_SCHEMA).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.PUT, schemaUrl(), ADMIN, COMPANY_SCHEMA).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private String url() {
        return PATH + "/" + datasetId;
    }

    private String schemaUrl() {
        return url() + "/schema";
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private static HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
