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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class StudioLessonApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private String courseId;
    private String chapterId;

    @BeforeEach
    void createChapter() throws Exception {
        new CourseFixtures(jdbcTemplate).clear();
        courseId =
                body(send(
                                HttpMethod.POST,
                                "/api/studio/courses",
                                "{\"slug\": \"sql-moi\", \"title\": \"SQL mới\","
                                        + " \"summary\": \"Tóm tắt.\","
                                        + " \"difficulty\": \"EASY\","
                                        + " \"estimatedMinutes\": 30}",
                                SETTER))
                        .get("id")
                        .asText();
        chapterId =
                body(send(
                                HttpMethod.POST,
                                "/api/studio/courses/" + courseId + "/chapters",
                                "{\"title\": \"Làm quen\"}",
                                SETTER))
                        .get("id")
                        .asText();
    }

    @Test
    @DisplayName("Thêm bài giảng nối vào cuối chương, đọc và sửa được")
    void lessons_areAddedReadAndUpdated() throws Exception {
        String first = idOf(addLesson(chapterId, "select-co-ban", SETTER));
        addLesson(chapterId, "loc-du-lieu", SETTER);

        JsonNode updated =
                body(
                        send(
                                HttpMethod.PATCH,
                                "/api/studio/lessons/" + first,
                                lessonJson("select-dau-tien", "SELECT đầu tiên", "# Mở đầu"),
                                SETTER));
        JsonNode read = body(send(HttpMethod.GET, "/api/studio/lessons/" + first, null, SETTER));
        JsonNode outline =
                body(
                        send(
                                HttpMethod.GET,
                                "/api/studio/courses/" + courseId + "/chapters",
                                null,
                                SETTER));

        assertThat(updated.get("slug").asText()).isEqualTo("select-dau-tien");
        assertThat(read.get("title").asText()).isEqualTo("SELECT đầu tiên");
        assertThat(read.get("body").asText()).isEqualTo("# Mở đầu");
        assertThat(read.get("chapterId").asText()).isEqualTo(chapterId);
        assertThat(outline.get(0).get("lessons"))
                .extracting(lesson -> lesson.get("slug").asText())
                .containsExactly("select-dau-tien", "loc-du-lieu");
    }

    @Test
    @DisplayName("Slug trùng trong cùng khoá trả 409, slug sai dạng hoặc nội dung quá dài trả 400")
    void lessonSlugAndBody_areValidated() throws Exception {
        String other =
                body(send(
                                HttpMethod.POST,
                                "/api/studio/courses/" + courseId + "/chapters",
                                "{\"title\": \"Chương 2\"}",
                                SETTER))
                        .get("id")
                        .asText();
        String lesson = idOf(addLesson(chapterId, "bai-1", SETTER));
        idOf(addLesson(other, "bai-2", SETTER));

        ResponseEntity<String> duplicate = addLesson(other, "bai-1", SETTER);
        ResponseEntity<String> renamedToTaken =
                send(
                        HttpMethod.PATCH,
                        "/api/studio/lessons/" + lesson,
                        lessonJson("bai-2", "Bài 1", ""),
                        SETTER);
        ResponseEntity<String> keepsOwnSlug =
                send(
                        HttpMethod.PATCH,
                        "/api/studio/lessons/" + lesson,
                        lessonJson("bai-1", "Bài 1 mới", ""),
                        SETTER);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody()).contains("\"code\":\"SLUG_TAKEN\"");
        assertThat(renamedToTaken.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(keepsOwnSlug.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(addLesson(chapterId, "Bai Sai", SETTER).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(
                        send(
                                        HttpMethod.POST,
                                        "/api/studio/chapters/" + chapterId + "/lessons",
                                        lessonJson("dai-qua", "Dài", "x".repeat(51_201)),
                                        SETTER)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Xoá bài giảng xoá luôn ô SQL và câu hỏi quiz của bài")
    void delete_removesLessonWithItsContent() throws Exception {
        String lesson = idOf(addLesson(chapterId, "bai-1", SETTER));
        jdbcTemplate.update(
                "INSERT INTO lesson_cells (id, lesson_id, cell_key, position, starter_sql,"
                        + " dialect, dataset_variant_id, dataset_version_id, dataset_checksum,"
                        + " created_at, updated_at) VALUES (UUID_TO_BIN(UUID()), UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(UUID()), 1, 'SELECT 1', 'POSTGRESQL',"
                        + " UUID_TO_BIN(UUID()), UUID_TO_BIN(UUID()), 'abc', NOW(6), NOW(6))",
                lesson);
        String question = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO quiz_questions (id, lesson_id, question_key, position, prompt,"
                        + " is_multiple, created_at, updated_at) VALUES (UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(UUID()), 1, 'Câu hỏi', FALSE, NOW(6),"
                        + " NOW(6))",
                question,
                lesson);
        jdbcTemplate.update(
                "INSERT INTO quiz_options (id, question_id, option_key, position, label,"
                        + " is_correct, created_at, updated_at) VALUES (UUID_TO_BIN(UUID()),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(UUID()), 1, 'Đúng', TRUE, NOW(6),"
                        + " NOW(6))",
                question);

        ResponseEntity<String> deleted =
                send(HttpMethod.DELETE, "/api/studio/lessons/" + lesson, null, SETTER);

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(count("lessons")).isZero();
        assertThat(count("lesson_cells")).isZero();
        assertThat(count("quiz_questions")).isZero();
        assertThat(count("quiz_options")).isZero();
    }

    @Test
    @DisplayName("SETTER khác nhận 404, bản chờ duyệt bị khoá sửa")
    void lessonRules_areEnforced() throws Exception {
        String lesson = idOf(addLesson(chapterId, "bai-1", SETTER));

        assertThat(addLesson(chapterId, "bai-la", OTHER_SETTER).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        send(HttpMethod.GET, "/api/studio/lessons/" + lesson, null, OTHER_SETTER)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        send(HttpMethod.DELETE, "/api/studio/lessons/" + lesson, null, OTHER_SETTER)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        jdbcTemplate.update("UPDATE course_revisions SET status = 'IN_REVIEW'");
        ResponseEntity<String> locked =
                send(
                        HttpMethod.PATCH,
                        "/api/studio/lessons/" + lesson,
                        lessonJson("bai-1", "Bài 1", ""),
                        SETTER);
        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(locked.getBody()).contains("\"code\":\"COURSE_LOCKED\"");
        assertThat(
                        send(HttpMethod.GET, "/api/studio/lessons/" + lesson, null, SETTER)
                                .getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<String> addLesson(String chapter, String slug, String token)
            throws Exception {
        return send(
                HttpMethod.POST,
                "/api/studio/chapters/" + chapter + "/lessons",
                lessonJson(slug, "Bài " + slug, "Nội dung"),
                token);
    }

    private String lessonJson(String slug, String title, String body) throws Exception {
        return objectMapper.writeValueAsString(Map.of("slug", slug, "title", title, "body", body));
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
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
