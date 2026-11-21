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
class CourseReviewApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private String courseId;
    private String chapterId;

    @BeforeEach
    void createCourse() throws Exception {
        new CourseFixtures(jdbcTemplate).clear();
        courseId =
                idOf(
                        send(
                                HttpMethod.POST,
                                "/api/studio/courses",
                                "{\"slug\": \"sql-moi\", \"title\": \"SQL mới\","
                                        + " \"summary\": \"Tóm tắt.\", \"difficulty\": \"EASY\"}",
                                SETTER));
        chapterId =
                idOf(
                        send(
                                HttpMethod.POST,
                                "/api/studio/courses/" + courseId + "/chapters",
                                "{\"title\": \"Làm quen\"}",
                                SETTER));
    }

    @Test
    @DisplayName("Gửi duyệt khoá bài giảng, khoá bị khoá sửa và hiện trong danh sách chờ duyệt")
    void submit_movesDraftToReviewQueue() throws Exception {
        addLesson();

        JsonNode submitted = body(submit(SETTER));
        JsonNode queue = body(send(HttpMethod.GET, "/api/admin/courses", null, ADMIN));

        assertThat(submitted.get("revisionStatus").asText()).isEqualTo("IN_REVIEW");
        assertThat(submitted.get("submittedAt").isNull()).isFalse();
        assertThat(queue.get("totalItems").asInt()).isEqualTo(1);
        assertThat(queue.get("items").get(0).get("courseId").asText()).isEqualTo(courseId);
        assertThat(queue.get("items").get(0).get("lessonCount").asInt()).isEqualTo(1);
        ResponseEntity<String> again = submit(SETTER);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(again.getBody()).contains("\"code\":\"COURSE_LOCKED\"");
    }

    @Test
    @DisplayName("Khoá chưa có bài giảng không gửi duyệt được, SETTER khác nhận 404")
    void submit_requiresLessonsAndOwnership() {
        ResponseEntity<String> empty = submit(SETTER);

        assertThat(empty.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(empty.getBody()).contains("\"code\":\"COURSE_EMPTY\"");
        assertThat(submit(OTHER_SETTER).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("ADMIN duyệt thì khoá xuất bản, người học thấy trong danh sách khoá")
    void approve_publishesCourse() throws Exception {
        addLesson();
        submit(SETTER);

        JsonNode approved = body(send(HttpMethod.POST, adminPath("approve"), null, ADMIN));
        JsonNode catalogue =
                objectMapper.readTree(restTemplate.getForObject("/api/courses", String.class));

        assertThat(approved.get("status").asText()).isEqualTo("PUBLISHED");
        assertThat(approved.get("revisionStatus").asText()).isEqualTo("PUBLISHED");
        assertThat(catalogue.get("items"))
                .extracting(item -> item.get("slug").asText())
                .containsExactly("sql-moi");
        assertThat(jdbcTemplate.queryForObject("SELECT decision FROM course_reviews", String.class))
                .isEqualTo("APPROVED");
        ResponseEntity<String> twice = send(HttpMethod.POST, adminPath("approve"), null, ADMIN);
        assertThat(twice.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(twice.getBody()).contains("\"code\":\"COURSE_NOT_IN_REVIEW\"");
    }

    @Test
    @DisplayName("ADMIN yêu cầu sửa kèm lý do, khoá về bản nháp và người soạn thấy lý do")
    void requestChanges_returnsDraftWithReason() throws Exception {
        addLesson();
        submit(SETTER);

        ResponseEntity<String> noReason =
                send(HttpMethod.POST, adminPath("request-changes"), "{\"reason\": \" \"}", ADMIN);
        JsonNode returned =
                body(
                        send(
                                HttpMethod.POST,
                                adminPath("request-changes"),
                                "{\"reason\": \"Thêm ví dụ cho bài 1\"}",
                                ADMIN));
        JsonNode studio =
                body(send(HttpMethod.GET, "/api/studio/courses/" + courseId, null, SETTER));

        assertThat(noReason.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(returned.get("revisionStatus").asText()).isEqualTo("DRAFT");
        assertThat(studio.get("latestReview").get("decision").asText())
                .isEqualTo("CHANGES_REQUESTED");
        assertThat(studio.get("latestReview").get("reason").asText())
                .isEqualTo("Thêm ví dụ cho bài 1");
        assertThat(
                        send(
                                        HttpMethod.POST,
                                        "/api/studio/courses/" + courseId + "/chapters",
                                        "{\"title\": \"Chương 2\"}",
                                        SETTER)
                                .getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("Chỉ ADMIN xem và xử lý hàng chờ duyệt")
    void reviewQueue_isAdminOnly() {
        assertThat(send(HttpMethod.GET, "/api/admin/courses", null, SETTER).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.POST, adminPath("approve"), null, SETTER).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void addLesson() throws Exception {
        idOf(
                send(
                        HttpMethod.POST,
                        "/api/studio/chapters/" + chapterId + "/lessons",
                        "{\"slug\": \"bai-1\", \"title\": \"Bài 1\", \"body\": \"Nội dung\","
                                + " \"estimatedMinutes\": 10}",
                        SETTER));
    }

    private ResponseEntity<String> submit(String token) {
        return send(HttpMethod.POST, "/api/studio/courses/" + courseId + "/submit", null, token);
    }

    private String adminPath(String action) {
        return "/api/admin/courses/" + courseId + "/" + action;
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
