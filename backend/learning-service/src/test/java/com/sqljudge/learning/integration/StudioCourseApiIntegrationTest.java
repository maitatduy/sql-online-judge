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
class StudioCourseApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String NEW_COURSE =
            """
            {"slug": "window-function", "title": "Window function",
             "summary": "Xếp hạng và cộng dồn.", "difficulty": "HARD"}
            """;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void clearCourses() {
        new CourseFixtures(jdbcTemplate).clear();
    }

    @Test
    @DisplayName("SETTER tạo khoá nháp, thấy nó trong Studio nhưng khách chưa thấy")
    void create_draftCourse_isListedInStudioOnly() throws Exception {
        ResponseEntity<String> created =
                send(HttpMethod.POST, "/api/studio/courses", NEW_COURSE, SETTER);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode course = objectMapper.readTree(created.getBody());
        assertThat(created.getHeaders().getLocation())
                .hasPath("/api/studio/courses/" + course.get("id").asText());
        assertThat(course.get("slug").asText()).isEqualTo("window-function");
        assertThat(course.get("status").asText()).isEqualTo("DRAFT");
        assertThat(course.get("revisionStatus").asText()).isEqualTo("DRAFT");
        assertThat(course.get("title").asText()).isEqualTo("Window function");
        assertThat(course.get("difficulty").asText()).isEqualTo("HARD");
        assertThat(course.get("estimatedMinutes").asInt()).isZero();
        assertThat(course.get("lessonCount").asInt()).isZero();

        JsonNode mine = body(send(HttpMethod.GET, "/api/studio/courses", null, SETTER));
        JsonNode others = body(send(HttpMethod.GET, "/api/studio/courses", null, OTHER_SETTER));
        assertThat(mine)
                .extracting(item -> item.get("slug").asText())
                .containsExactly("window-function");
        assertThat(others).isEmpty();
        assertThat(restTemplate.getForObject("/api/courses", String.class))
                .contains("\"totalItems\":0");
    }

    @Test
    @DisplayName("Slug trùng trả 409, dữ liệu sai trả 400")
    void create_invalidOrDuplicate_isRejected() {
        send(HttpMethod.POST, "/api/studio/courses", NEW_COURSE, SETTER);

        ResponseEntity<String> duplicate =
                send(HttpMethod.POST, "/api/studio/courses", NEW_COURSE, OTHER_SETTER);
        ResponseEntity<String> invalid =
                send(
                        HttpMethod.POST,
                        "/api/studio/courses",
                        "{\"slug\": \"Sai Slug\", \"title\": \"\", \"summary\": \"x\","
                                + " \"difficulty\": \"EASY\"}",
                        SETTER);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody()).contains("\"code\":\"SLUG_TAKEN\"");
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody())
                .contains("\"field\":\"slug\"")
                .contains("\"field\":\"title\"");
    }

    @Test
    @DisplayName("Chủ khoá và ADMIN xem được khoá nháp, SETTER khác nhận 404, LEARNER nhận 403")
    void get_isLimitedToOwnerAndAdmin() throws Exception {
        String id = idOf(send(HttpMethod.POST, "/api/studio/courses", NEW_COURSE, SETTER));
        String path = "/api/studio/courses/" + id;

        assertThat(send(HttpMethod.GET, path, null, SETTER).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(send(HttpMethod.GET, path, null, ADMIN).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        ResponseEntity<String> other = send(HttpMethod.GET, path, null, OTHER_SETTER);
        assertThat(other.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(other.getBody()).contains("\"code\":\"COURSE_NOT_FOUND\"");
        assertThat(send(HttpMethod.GET, path, null, TestJwts.learnerToken()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(restTemplate.getForEntity("/api/studio/courses", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Sửa thông tin bản nháp, bản đang chờ duyệt thì bị khoá sửa")
    void update_draftMetadata_untilSubmitted() throws Exception {
        String id = idOf(send(HttpMethod.POST, "/api/studio/courses", NEW_COURSE, SETTER));
        String path = "/api/studio/courses/" + id;
        String change =
                "{\"title\": \"Window function nâng cao\", \"summary\": \"Mới.\","
                        + " \"difficulty\": \"MEDIUM\"}";

        JsonNode updated = body(send(HttpMethod.PATCH, path, change, SETTER));
        jdbcTemplate.update("UPDATE course_revisions SET status = 'IN_REVIEW'");
        ResponseEntity<String> locked = send(HttpMethod.PATCH, path, change, SETTER);

        assertThat(updated.get("title").asText()).isEqualTo("Window function nâng cao");
        assertThat(updated.get("difficulty").asText()).isEqualTo("MEDIUM");
        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(locked.getBody()).contains("\"code\":\"COURSE_LOCKED\"");
    }

    private String idOf(ResponseEntity<String> response) throws Exception {
        return body(response).get("id").asText();
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
