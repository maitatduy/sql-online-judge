package com.sqljudge.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class LessonApiIntegrationTest extends ContainerSupport {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private CourseFixtures fixtures;

    @BeforeEach
    void clearCourses() {
        fixtures = new CourseFixtures(jdbcTemplate);
        fixtures.clear();
    }

    @Test
    @DisplayName("Người đã đăng nhập đọc được bài giảng của khoá đã xuất bản")
    void read_publishedLesson_returnsLessonWithCourseContext() throws Exception {
        String revisionId = fixtures.publishedCourse("sql-co-ban", "EASY", "2026-11-01 08:00:00");
        String chapterId = fixtures.chapter(revisionId, "Làm quen với SELECT", 1);
        String lessonKey = fixtures.lesson(chapterId, "select-la-gi", 1);

        ResponseEntity<String> response =
                getAsLearner("/api/courses/sql-co-ban/lessons/select-la-gi");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode lesson = objectMapper.readTree(response.getBody());
        assertThat(lesson.get("lessonKey").asText()).isEqualTo(lessonKey);
        assertThat(lesson.get("slug").asText()).isEqualTo("select-la-gi");
        assertThat(lesson.get("title").asText()).isEqualTo("Bài select-la-gi");
        assertThat(lesson.get("body").asText()).isEqualTo("# Nội dung select-la-gi");
        assertThat(lesson.get("chapterTitle").asText()).isEqualTo("Làm quen với SELECT");
        assertThat(lesson.get("courseSlug").asText()).isEqualTo("sql-co-ban");
        assertThat(lesson.get("courseTitle").asText()).isEqualTo("Khoá sql-co-ban");
    }

    @Test
    @DisplayName("Khách chưa đăng nhập bị từ chối với 401")
    void read_anonymous_returnsUnauthorized() {
        String revisionId = fixtures.publishedCourse("sql-co-ban", "EASY", "2026-11-01 08:00:00");
        fixtures.lesson(fixtures.chapter(revisionId, "Chương", 1), "select-la-gi", 1);

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "/api/courses/sql-co-ban/lessons/select-la-gi", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Bài của khoá chưa xuất bản hoặc bài không tồn tại trả 404")
    void read_hiddenOrMissingLesson_returnsNotFound() {
        String draftRevision = fixtures.course("ban-nhap", "DRAFT", "EASY", null, false);
        fixtures.lesson(fixtures.chapter(draftRevision, "Chương", 1), "bai-nhap", 1);
        fixtures.publishedCourse("sql-co-ban", "EASY", "2026-11-01 08:00:00");

        for (String path :
                new String[] {
                    "/api/courses/ban-nhap/lessons/bai-nhap",
                    "/api/courses/sql-co-ban/lessons/khong-co"
                }) {
            ResponseEntity<String> response = getAsLearner(path);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).contains("\"code\":\"LESSON_NOT_FOUND\"");
        }
    }

    private ResponseEntity<String> getAsLearner(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwts.learnerToken());
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
