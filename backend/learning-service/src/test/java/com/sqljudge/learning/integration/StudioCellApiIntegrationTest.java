package com.sqljudge.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class StudioCellApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final UUID VERSION_ID = UUID.fromString("0192f3f6-0000-7000-8000-000000000001");
    private static final UUID PUBLIC_VARIANT =
            UUID.fromString("0192f3f6-0000-7000-8000-000000000002");
    private static final UUID HIDDEN_VARIANT =
            UUID.fromString("0192f3f6-0000-7000-8000-000000000003");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private String cellsPath;

    @DynamicPropertySource
    static void problemServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.problem-service-url", ProblemServiceStub::url);
    }

    @BeforeEach
    void createLesson() throws Exception {
        ProblemServiceStub.variant(PUBLIC_VARIANT, VERSION_ID, true, true);
        ProblemServiceStub.variant(HIDDEN_VARIANT, VERSION_ID, false, true);
        new CourseFixtures(jdbcTemplate).clear();
        String courseId =
                idOf(
                        send(
                                HttpMethod.POST,
                                "/api/studio/courses",
                                "{\"slug\": \"sql-moi\", \"title\": \"SQL mới\","
                                        + " \"summary\": \"Tóm tắt.\", \"difficulty\": \"EASY\"}",
                                SETTER));
        String chapterId =
                idOf(
                        send(
                                HttpMethod.POST,
                                "/api/studio/courses/" + courseId + "/chapters",
                                "{\"title\": \"Làm quen\"}",
                                SETTER));
        String lessonId =
                idOf(
                        send(
                                HttpMethod.POST,
                                "/api/studio/chapters/" + chapterId + "/lessons",
                                "{\"slug\": \"bai-1\", \"title\": \"Bài 1\", \"body\": \"\","
                                        + " \"estimatedMinutes\": 10}",
                                SETTER));
        cellsPath = "/api/studio/lessons/" + lessonId + "/cells";
    }

    @Test
    @DisplayName("Thêm ô SQL nối vào cuối bài, lưu đúng bản và checksum của bộ dữ liệu")
    void cells_areAddedInOrderWithSnapshotReference() throws Exception {
        String first = idOf(addCell("SELECT * FROM departments;", "POSTGRESQL", SETTER));
        addCell("SELECT name FROM employees;", "MYSQL", SETTER);

        JsonNode cells = body(send(HttpMethod.GET, cellsPath, null, SETTER));

        assertThat(cells)
                .extracting(cell -> cell.get("starterSql").asText())
                .containsExactly("SELECT * FROM departments;", "SELECT name FROM employees;");
        assertThat(cells.get(0).get("id").asText()).isEqualTo(first);
        assertThat(cells.get(0).get("dialect").asText()).isEqualTo("POSTGRESQL");
        assertThat(cells.get(0).get("datasetVariantId").asText())
                .isEqualTo(PUBLIC_VARIANT.toString());
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT dataset_checksum FROM lesson_cells"
                                        + " WHERE id = UUID_TO_BIN(?)",
                                String.class,
                                first))
                .isEqualTo(ProblemServiceStub.checksumOf(VERSION_ID));
    }

    @Test
    @DisplayName("Sửa câu SQL và dialect của ô, xoá ô")
    void cells_areUpdatedAndDeleted() throws Exception {
        String cell = idOf(addCell("SELECT 1;", "POSTGRESQL", SETTER));

        JsonNode updated =
                body(
                        send(
                                HttpMethod.PATCH,
                                "/api/studio/cells/" + cell,
                                cellJson("SELECT 2;", "SQLITE", PUBLIC_VARIANT),
                                SETTER));
        ResponseEntity<String> deleted =
                send(HttpMethod.DELETE, "/api/studio/cells/" + cell, null, SETTER);

        assertThat(updated.get("starterSql").asText()).isEqualTo("SELECT 2;");
        assertThat(updated.get("dialect").asText()).isEqualTo("SQLITE");
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(body(send(HttpMethod.GET, cellsPath, null, SETTER))).isEmpty();
    }

    @Test
    @DisplayName("Bộ dữ liệu ẩn trả 422, câu SQL trống trả 400, SETTER khác nhận 404")
    void cellRules_areEnforced() throws Exception {
        ResponseEntity<String> hidden =
                send(
                        HttpMethod.POST,
                        cellsPath,
                        cellJson("SELECT 1;", "POSTGRESQL", HIDDEN_VARIANT),
                        SETTER);

        assertThat(hidden.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(hidden.getBody()).contains("\"code\":\"DATASET_VARIANT_NOT_PUBLIC\"");
        assertThat(addCell(" ", "POSTGRESQL", SETTER).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(addCell("SELECT 1;", "POSTGRESQL", OTHER_SETTER).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.GET, cellsPath, null, OTHER_SETTER).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Bản đang chờ duyệt bị khoá sửa ô SQL")
    void cells_lockedWhileInReview() throws Exception {
        String cell = idOf(addCell("SELECT 1;", "POSTGRESQL", SETTER));
        jdbcTemplate.update("UPDATE course_revisions SET status = 'IN_REVIEW'");

        ResponseEntity<String> added = addCell("SELECT 2;", "POSTGRESQL", SETTER);
        ResponseEntity<String> deleted =
                send(HttpMethod.DELETE, "/api/studio/cells/" + cell, null, SETTER);

        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(added.getBody()).contains("\"code\":\"COURSE_LOCKED\"");
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(body(send(HttpMethod.GET, cellsPath, null, SETTER))).hasSize(1);
    }

    private ResponseEntity<String> addCell(String sql, String dialect, String token)
            throws Exception {
        return send(HttpMethod.POST, cellsPath, cellJson(sql, dialect, PUBLIC_VARIANT), token);
    }

    private String cellJson(String sql, String dialect, UUID variantId) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("starterSql", sql, "dialect", dialect, "datasetVariantId", variantId));
    }

    private String idOf(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).get("id").asText();
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return objectMapper.readTree(response.getBody());
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String json, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(json, headers), String.class);
    }
}
