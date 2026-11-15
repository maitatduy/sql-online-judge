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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CourseDetailApiIntegrationTest extends ContainerSupport {

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
    @DisplayName("Khách xem được đề cương khoá đã xuất bản, chương và bài theo đúng thứ tự")
    void detail_publishedCourse_returnsOrderedOutline() throws Exception {
        String revisionId = fixtures.publishedCourse("sql-co-ban", "EASY", "2026-11-01 08:00:00");
        String secondChapter = fixtures.chapter(revisionId, "Lọc dữ liệu", 2);
        String firstChapter = fixtures.chapter(revisionId, "Làm quen với SELECT", 1);
        fixtures.lesson(firstChapter, "chon-cot", 2);
        String firstLessonKey = fixtures.lesson(firstChapter, "select-la-gi", 1);
        fixtures.lesson(secondChapter, "menh-de-where", 1);

        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/courses/sql-co-ban", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode course = objectMapper.readTree(response.getBody());
        assertThat(course.get("slug").asText()).isEqualTo("sql-co-ban");
        assertThat(course.get("title").asText()).isEqualTo("Khoá sql-co-ban");
        assertThat(course.get("difficulty").asText()).isEqualTo("EASY");
        assertThat(course.get("publishedAt").asText()).isEqualTo("2026-11-01T08:00:00Z");
        JsonNode chapters = course.get("chapters");
        assertThat(chapters)
                .extracting(chapter -> chapter.get("title").asText())
                .containsExactly("Làm quen với SELECT", "Lọc dữ liệu");
        JsonNode firstLessons = chapters.get(0).get("lessons");
        assertThat(firstLessons)
                .extracting(lesson -> lesson.get("slug").asText())
                .containsExactly("select-la-gi", "chon-cot");
        assertThat(firstLessons.get(0).get("lessonKey").asText()).isEqualTo(firstLessonKey);
        assertThat(firstLessons.get(0).get("title").asText()).isEqualTo("Bài select-la-gi");
        assertThat(firstLessons.get(0).has("body")).isFalse();
    }

    @Test
    @DisplayName("Khoá nháp, đã ẩn hoặc đã xoá trả 404 như khoá không tồn tại")
    void detail_unpublishedCourses_returnNotFound() {
        fixtures.course("ban-nhap", "DRAFT", "EASY", null, false);
        fixtures.course("da-an", "ARCHIVED", "EASY", "2026-10-01 08:00:00", false);
        fixtures.course("da-xoa", "PUBLISHED", "EASY", "2026-10-02 08:00:00", true);

        for (String slug : new String[] {"ban-nhap", "da-an", "da-xoa", "khong-co"}) {
            ResponseEntity<String> response =
                    restTemplate.getForEntity("/api/courses/" + slug, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).contains("\"code\":\"COURSE_NOT_FOUND\"");
        }
    }
}
