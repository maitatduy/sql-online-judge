package com.sqljudge.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class CourseDraftApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private String courseId;
    private String lessonId;

    @BeforeEach
    void publishCourse() throws Exception {
        new CourseFixtures(jdbcTemplate).clear();
        courseId =
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
        lessonId =
                idOf(
                        send(
                                HttpMethod.POST,
                                "/api/studio/chapters/" + chapterId + "/lessons",
                                lessonJson("Bài đầu tiên"),
                                SETTER));
        addCellAndQuiz(lessonId);
        send(HttpMethod.POST, "/api/studio/courses/" + courseId + "/submit", null, SETTER);
        send(HttpMethod.POST, "/api/admin/courses/" + courseId + "/approve", null, ADMIN);
    }

    @Test
    @DisplayName("Tạo bản nháp từ bản đã xuất bản, chép đủ nội dung và giữ nguyên các khoá")
    void draft_copiesPublishedContentWithStableKeys() throws Exception {
        ResponseEntity<String> created = createDraft(SETTER);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode draft = objectMapper.readTree(created.getBody());
        assertThat(draft.get("revisionStatus").asText()).isEqualTo("DRAFT");
        assertThat(draft.get("status").asText()).isEqualTo("PUBLISHED");
        assertThat(draft.get("lessonCount").asInt()).isEqualTo(1);
        assertThat(count("SELECT COUNT(DISTINCT lesson_key) FROM lessons")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM lessons")).isEqualTo(2);
        assertThat(count("SELECT COUNT(DISTINCT cell_key) FROM lesson_cells")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM lesson_cells")).isEqualTo(2);
        assertThat(count("SELECT COUNT(DISTINCT option_key) FROM quiz_options")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM quiz_options")).isEqualTo(4);
    }

    @Test
    @DisplayName("Sửa bản nháp không đổi bản người học đang xem cho tới khi được duyệt lại")
    void draftChanges_reachLearnersOnlyAfterApproval() throws Exception {
        createDraft(SETTER);
        String draftLessonId =
                body(send(
                                HttpMethod.GET,
                                "/api/studio/courses/" + courseId + "/chapters",
                                null,
                                SETTER))
                        .get(0)
                        .get("lessons")
                        .get(0)
                        .get("id")
                        .asText();

        send(
                HttpMethod.PATCH,
                "/api/studio/lessons/" + draftLessonId,
                lessonJson("Bài đã sửa"),
                SETTER);
        assertThat(publicLessonTitle()).isEqualTo("Bài đầu tiên");

        send(HttpMethod.POST, "/api/studio/courses/" + courseId + "/submit", null, SETTER);
        send(HttpMethod.POST, "/api/admin/courses/" + courseId + "/approve", null, ADMIN);

        assertThat(publicLessonTitle()).isEqualTo("Bài đã sửa");
        assertThat(count("SELECT COUNT(*) FROM course_revisions WHERE status = 'SUPERSEDED'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Đã có bản nháp trả 409, khoá chưa xuất bản trả 422, SETTER khác nhận 404")
    void draftRules_areEnforced() throws Exception {
        createDraft(SETTER);

        ResponseEntity<String> again = createDraft(SETTER);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(again.getBody()).contains("\"code\":\"COURSE_DRAFT_EXISTS\"");
        assertThat(createDraft(OTHER_SETTER).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        String unpublished =
                idOf(
                        send(
                                HttpMethod.POST,
                                "/api/studio/courses",
                                "{\"slug\": \"chua-xuat-ban\", \"title\": \"Nháp\","
                                        + " \"summary\": \"Tóm tắt.\", \"difficulty\": \"EASY\"}",
                                SETTER));
        ResponseEntity<String> notPublished =
                send(
                        HttpMethod.POST,
                        "/api/studio/courses/" + unpublished + "/draft",
                        null,
                        SETTER);
        assertThat(notPublished.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(notPublished.getBody()).contains("\"code\":\"COURSE_NOT_PUBLISHED\"");
    }

    private void addCellAndQuiz(String lesson) {
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
        for (int position = 1; position <= 2; position++) {
            jdbcTemplate.update(
                    "INSERT INTO quiz_options (id, question_id, option_key, position, label,"
                            + " is_correct, created_at, updated_at) VALUES (UUID_TO_BIN(UUID()),"
                            + " UUID_TO_BIN(?), UUID_TO_BIN(UUID()), ?, ?, ?, NOW(6), NOW(6))",
                    question,
                    position,
                    "Lựa chọn " + position,
                    position == 1);
        }
    }

    private String publicLessonTitle() throws Exception {
        return objectMapper
                .readTree(restTemplate.getForObject("/api/courses/sql-moi", String.class))
                .get("chapters")
                .get(0)
                .get("lessons")
                .get(0)
                .get("title")
                .asText();
    }

    private ResponseEntity<String> createDraft(String token) {
        return send(HttpMethod.POST, "/api/studio/courses/" + courseId + "/draft", null, token);
    }

    private static String lessonJson(String title) {
        return "{\"slug\": \"bai-1\", \"title\": \""
                + title
                + "\", \"body\": \"Nội dung\","
                + " \"estimatedMinutes\": 10}";
    }

    private int count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
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
