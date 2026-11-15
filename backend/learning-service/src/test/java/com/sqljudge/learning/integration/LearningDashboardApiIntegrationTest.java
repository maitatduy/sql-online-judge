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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class LearningDashboardApiIntegrationTest extends ContainerSupport {

    private static final UUID OTHER_LEARNER_ID =
            UUID.fromString("0192f3a0-0000-7000-8000-000000000002");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private CourseFixtures fixtures;
    private String basicFirstLesson;

    @BeforeEach
    void createCourses() {
        fixtures = new CourseFixtures(jdbcTemplate);
        fixtures.clear();
        String basic = fixtures.publishedCourse("sql-co-ban", "EASY", "2026-11-01 08:00:00");
        String basicChapter = fixtures.chapter(basic, "Chương", 1);
        basicFirstLesson = fixtures.lesson(basicChapter, "bai-1", 1);
        fixtures.lesson(basicChapter, "bai-2", 2);
        String joins = fixtures.publishedCourse("join", "MEDIUM", "2026-11-02 08:00:00");
        fixtures.lesson(fixtures.chapter(joins, "Chương", 1), "inner-join", 1);
        fixtures.publishedCourse("chua-hoc", "HARD", "2026-11-03 08:00:00");
    }

    @Test
    @DisplayName(
            "Danh sách khoá đang học chỉ gồm khoá mình đã đăng ký, kèm tiến độ và bài tiếp theo")
    void enrollments_listOwnCoursesWithProgress() throws Exception {
        send(HttpMethod.POST, "/api/courses/join/enrollment", TestJwts.learnerToken());
        send(
                HttpMethod.POST,
                "/api/courses/sql-co-ban/lessons/bai-1/completion",
                TestJwts.learnerToken());
        send(
                HttpMethod.POST,
                "/api/courses/chua-hoc/enrollment",
                TestJwts.token(OTHER_LEARNER_ID, "LEARNER"));

        JsonNode enrollments = send(HttpMethod.GET, "/api/enrollments", TestJwts.learnerToken());

        assertThat(enrollments)
                .extracting(course -> course.get("slug").asText())
                .containsExactlyInAnyOrder("sql-co-ban", "join");
        JsonNode basic = enrollmentOf(enrollments, "sql-co-ban");
        assertThat(basic.get("title").asText()).isEqualTo("Khoá sql-co-ban");
        assertThat(basic.get("difficulty").asText()).isEqualTo("EASY");
        assertThat(basic.get("completedCount").asInt()).isEqualTo(1);
        assertThat(basic.get("lessonCount").asInt()).isEqualTo(2);
        assertThat(basic.get("nextLessonSlug").asText()).isEqualTo("bai-2");
        assertThat(enrollmentOf(enrollments, "join").get("completedCount").asInt()).isZero();
    }

    @Test
    @DisplayName("Hoạt động học gom số bài hoàn thành theo ngày giờ Việt Nam, từ ngày được hỏi")
    void activity_groupsCompletionsByVietnamDay() throws Exception {
        completedAt("2026-11-10 18:30:00");
        completedAt("2026-11-11 02:00:00");
        completedAt("2026-11-12 09:00:00");
        completedAt("2026-11-01 09:00:00");

        JsonNode days =
                send(
                        HttpMethod.GET,
                        "/api/learning/activity?from=2026-11-05",
                        TestJwts.learnerToken());

        assertThat(days)
                .extracting(day -> day.get("date").asText() + "=" + day.get("count").asInt())
                .containsExactly("2026-11-11=2", "2026-11-12=1");
    }

    @Test
    @DisplayName("Khách bị từ chối với 401, khoảng thời gian quá một năm trả 400")
    void dashboardApis_rejectAnonymousAndHugeRanges() {
        assertThat(restTemplate.getForEntity("/api/enrollments", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        ResponseEntity<String> tooLong =
                exchange(
                        HttpMethod.GET,
                        "/api/learning/activity?from=2020-01-01",
                        TestJwts.learnerToken());
        assertThat(tooLong.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void completedAt(String instant) {
        jdbcTemplate.update(
                "INSERT INTO lesson_progress (id, course_id, user_id, lesson_key, completed_at,"
                    + " created_at, updated_at) SELECT UUID_TO_BIN(?), id, UUID_TO_BIN(?),"
                    + " UUID_TO_BIN(?), ?, NOW(6), NOW(6) FROM courses WHERE slug = 'sql-co-ban'",
                UUID.randomUUID().toString(),
                TestJwts.LEARNER_ID.toString(),
                UUID.randomUUID().toString(),
                instant);
    }

    private static JsonNode enrollmentOf(JsonNode enrollments, String slug) {
        for (JsonNode course : enrollments) {
            if (course.get("slug").asText().equals(slug)) {
                return course;
            }
        }
        throw new AssertionError("Không có khoá " + slug);
    }

    private JsonNode send(HttpMethod method, String path, String token) throws Exception {
        ResponseEntity<String> response = exchange(method, path, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, method, new HttpEntity<>(headers), String.class);
    }
}
