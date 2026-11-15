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
class StudioChapterApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private CourseFixtures fixtures;
    private String chaptersPath;

    @BeforeEach
    void createCourse() throws Exception {
        fixtures = new CourseFixtures(jdbcTemplate);
        fixtures.clear();
        String courseId =
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
        chaptersPath = "/api/studio/courses/" + courseId + "/chapters";
    }

    @Test
    @DisplayName("Thêm chương nối vào cuối, liệt kê theo thứ tự và sửa được tên")
    void chapters_areAppendedListedAndRenamed() throws Exception {
        String first = idOf(addChapter("Làm quen", SETTER));
        addChapter("Lọc dữ liệu", SETTER);

        JsonNode renamed =
                body(
                        send(
                                HttpMethod.PATCH,
                                "/api/studio/chapters/" + first,
                                "{\"title\": \"Làm quen với SELECT\"}",
                                SETTER));
        JsonNode chapters = body(send(HttpMethod.GET, chaptersPath, null, SETTER));

        assertThat(renamed.get("title").asText()).isEqualTo("Làm quen với SELECT");
        assertThat(chapters)
                .extracting(chapter -> chapter.get("title").asText())
                .containsExactly("Làm quen với SELECT", "Lọc dữ liệu");
        assertThat(chapters.get(0).get("lessons")).isEmpty();
    }

    @Test
    @DisplayName("Xoá được chương trống, chương còn bài giảng thì bị từ chối")
    void delete_onlyEmptyChapters() throws Exception {
        String empty = idOf(addChapter("Trống", SETTER));
        String withLesson = idOf(addChapter("Có bài", SETTER));
        fixtures.lesson(withLesson, "bai-1", 1);

        ResponseEntity<String> deleted =
                send(HttpMethod.DELETE, "/api/studio/chapters/" + empty, null, SETTER);
        ResponseEntity<String> refused =
                send(HttpMethod.DELETE, "/api/studio/chapters/" + withLesson, null, SETTER);

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(refused.getBody()).contains("\"code\":\"CHAPTER_NOT_EMPTY\"");
        assertThat(body(send(HttpMethod.GET, chaptersPath, null, SETTER)))
                .extracting(chapter -> chapter.get("title").asText())
                .containsExactly("Có bài");
    }

    @Test
    @DisplayName("SETTER khác nhận 404, tên trống trả 400, bản chờ duyệt bị khoá sửa")
    void chapterRules_areEnforced() throws Exception {
        String chapter = idOf(addChapter("Chương", SETTER));

        assertThat(addChapter("Chương lạ", OTHER_SETTER).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        send(
                                        HttpMethod.PATCH,
                                        "/api/studio/chapters/" + chapter,
                                        "{\"title\": \"x\"}",
                                        OTHER_SETTER)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(addChapter(" ", SETTER).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        jdbcTemplate.update("UPDATE course_revisions SET status = 'IN_REVIEW'");
        ResponseEntity<String> locked = addChapter("Sau khi gửi duyệt", SETTER);
        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(locked.getBody()).contains("\"code\":\"COURSE_LOCKED\"");
    }

    private ResponseEntity<String> addChapter(String title, String token) {
        return send(HttpMethod.POST, chaptersPath, "{\"title\": \"" + title + "\"}", token);
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
