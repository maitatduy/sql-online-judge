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
class LearningProgressApiIntegrationTest extends ContainerSupport {

    private static final UUID OTHER_LEARNER_ID =
            UUID.fromString("0192f3a0-0000-7000-8000-000000000002");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private CourseFixtures fixtures;
    private String firstLessonKey;

    @BeforeEach
    void createCourse() {
        fixtures = new CourseFixtures(jdbcTemplate);
        fixtures.clear();
        String revisionId = fixtures.publishedCourse("sql-co-ban", "EASY", "2026-11-01 08:00:00");
        String firstChapter = fixtures.chapter(revisionId, "Chương 1", 1);
        firstLessonKey = fixtures.lesson(firstChapter, "select-la-gi", 1);
        fixtures.lesson(firstChapter, "chon-cot", 2);
        fixtures.lesson(fixtures.chapter(revisionId, "Chương 2", 2), "menh-de-where", 1);
    }

    @Test
    @DisplayName("Chưa đăng ký thì tiến độ trống và bài tiếp theo là bài đầu tiên")
    void progress_beforeEnrolling_isEmpty() throws Exception {
        JsonNode progress = send(HttpMethod.GET, "/api/courses/sql-co-ban/progress", learner());

        assertThat(progress.get("enrolled").asBoolean()).isFalse();
        assertThat(progress.get("completedLessonKeys")).isEmpty();
        assertThat(progress.get("lessonCount").asInt()).isEqualTo(3);
        assertThat(progress.get("nextLessonSlug").asText()).isEqualTo("select-la-gi");
    }

    @Test
    @DisplayName("Đăng ký nhiều lần chỉ tạo một bản đăng ký")
    void enroll_twice_createsOneEnrollment() throws Exception {
        send(HttpMethod.POST, "/api/courses/sql-co-ban/enrollment", learner());
        JsonNode progress = send(HttpMethod.POST, "/api/courses/sql-co-ban/enrollment", learner());

        assertThat(progress.get("enrolled").asBoolean()).isTrue();
        assertThat(count("SELECT COUNT(*) FROM enrollments")).isEqualTo(1);
    }

    @Test
    @DisplayName("Hoàn thành bài tự đăng ký khoá, chuyển bài tiếp theo và không ghi trùng")
    void complete_lesson_recordsProgressOnce() throws Exception {
        send(HttpMethod.POST, completion("select-la-gi"), learner());
        JsonNode progress = send(HttpMethod.POST, completion("select-la-gi"), learner());

        assertThat(progress.get("enrolled").asBoolean()).isTrue();
        assertThat(progress.get("completedLessonKeys"))
                .extracting(JsonNode::asText)
                .containsExactly(firstLessonKey);
        assertThat(progress.get("nextLessonSlug").asText()).isEqualTo("chon-cot");
        assertThat(count("SELECT COUNT(*) FROM lesson_progress")).isEqualTo(1);
    }

    @Test
    @DisplayName("Hoàn thành mọi bài thì không còn bài tiếp theo")
    void complete_allLessons_hasNoNextLesson() throws Exception {
        send(HttpMethod.POST, completion("select-la-gi"), learner());
        send(HttpMethod.POST, completion("menh-de-where"), learner());
        JsonNode progress = send(HttpMethod.POST, completion("chon-cot"), learner());

        assertThat(progress.get("completedLessonKeys")).hasSize(3);
        assertThat(progress.get("nextLessonSlug").isNull()).isTrue();
    }

    @Test
    @DisplayName("Mỗi người chỉ thấy tiến độ của chính mình")
    void progress_isPerLearner() throws Exception {
        send(HttpMethod.POST, completion("select-la-gi"), learner());

        JsonNode otherProgress =
                send(
                        HttpMethod.GET,
                        "/api/courses/sql-co-ban/progress",
                        TestJwts.token(OTHER_LEARNER_ID, "LEARNER"));

        assertThat(otherProgress.get("enrolled").asBoolean()).isFalse();
        assertThat(otherProgress.get("completedLessonKeys")).isEmpty();
    }

    @Test
    @DisplayName("Khách bị từ chối, khoá hoặc bài không tồn tại trả 404")
    void progress_rejectsAnonymousAndUnknownTargets() {
        assertThat(
                        restTemplate
                                .postForEntity(completion("select-la-gi"), null, String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> unknownCourse =
                exchange(HttpMethod.GET, "/api/courses/khong-co/progress", learner());
        assertThat(unknownCourse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknownCourse.getBody()).contains("\"code\":\"COURSE_NOT_FOUND\"");

        ResponseEntity<String> unknownLesson =
                exchange(HttpMethod.POST, completion("khong-co"), learner());
        assertThat(unknownLesson.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknownLesson.getBody()).contains("\"code\":\"LESSON_NOT_FOUND\"");
    }

    private static String learner() {
        return TestJwts.learnerToken();
    }

    private static String completion(String lessonSlug) {
        return "/api/courses/sql-co-ban/lessons/" + lessonSlug + "/completion";
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

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
