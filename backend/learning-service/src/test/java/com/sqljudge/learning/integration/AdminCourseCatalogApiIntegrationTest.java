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
class AdminCourseCatalogApiIntegrationTest extends ContainerSupport {

    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String CATALOG = "/api/admin/courses/catalog";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private CourseFixtures fixtures;

    @BeforeEach
    void seedCourses() {
        fixtures = new CourseFixtures(jdbcTemplate);
        fixtures.clear();
        fixtures.publishedCourse("sql-co-ban", "EASY", "2026-11-01 08:00:00");
        fixtures.course("ban-nhap", "DRAFT", "MEDIUM", null, false);
        fixtures.course("da-xoa", "DRAFT", "MEDIUM", null, true);
    }

    @Test
    @DisplayName("ADMIN thấy mọi khoá chưa xoá, lọc được theo trạng thái")
    void catalog_listsCoursesAndFiltersByStatus() throws Exception {
        JsonNode all = body(send(HttpMethod.GET, CATALOG, ADMIN));
        JsonNode drafts = body(send(HttpMethod.GET, CATALOG + "?status=DRAFT", ADMIN));

        assertThat(all.get("totalItems").asInt()).isEqualTo(2);
        assertThat(all.get("items"))
                .extracting(item -> item.get("slug").asText())
                .containsExactlyInAnyOrder("sql-co-ban", "ban-nhap");
        assertThat(drafts.get("items"))
                .extracting(item -> item.get("slug").asText())
                .containsExactly("ban-nhap");
        JsonNode published = findBySlug(all, "sql-co-ban");
        assertThat(published.get("status").asText()).isEqualTo("PUBLISHED");
        assertThat(published.get("title").asText()).isEqualTo("Khoá sql-co-ban");
        assertThat(published.get("publishedAt").isNull()).isFalse();
    }

    @Test
    @DisplayName("Ẩn khoá thì người học không còn thấy, hiện lại thì thấy lại")
    void archiveAndRestore_toggleCatalogueVisibility() throws Exception {
        String courseId =
                findBySlug(body(send(HttpMethod.GET, CATALOG, ADMIN)), "sql-co-ban")
                        .get("courseId")
                        .asText();

        JsonNode archived =
                body(send(HttpMethod.POST, "/api/admin/courses/" + courseId + "/archive", ADMIN));
        assertThat(archived.get("status").asText()).isEqualTo("ARCHIVED");
        assertThat(restTemplate.getForObject("/api/courses", String.class))
                .contains("\"totalItems\":0");
        assertThat(
                        restTemplate
                                .getForEntity("/api/courses/sql-co-ban", String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        JsonNode restored =
                body(send(HttpMethod.POST, "/api/admin/courses/" + courseId + "/restore", ADMIN));
        assertThat(restored.get("status").asText()).isEqualTo("PUBLISHED");
        assertThat(restTemplate.getForObject("/api/courses", String.class))
                .contains("\"totalItems\":1");
    }

    @Test
    @DisplayName("Khoá nháp không ẩn được, khoá chưa ẩn không hiện lại được, SETTER nhận 403")
    void archiveRules_areEnforced() throws Exception {
        JsonNode all = body(send(HttpMethod.GET, CATALOG, ADMIN));
        String draftId = findBySlug(all, "ban-nhap").get("courseId").asText();
        String publishedId = findBySlug(all, "sql-co-ban").get("courseId").asText();

        ResponseEntity<String> archiveDraft =
                send(HttpMethod.POST, "/api/admin/courses/" + draftId + "/archive", ADMIN);
        ResponseEntity<String> restorePublished =
                send(HttpMethod.POST, "/api/admin/courses/" + publishedId + "/restore", ADMIN);

        assertThat(archiveDraft.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(archiveDraft.getBody()).contains("\"code\":\"COURSE_NOT_PUBLISHED\"");
        assertThat(restorePublished.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(restorePublished.getBody()).contains("\"code\":\"COURSE_NOT_ARCHIVED\"");
        assertThat(send(HttpMethod.GET, CATALOG, SETTER).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(
                        send(
                                        HttpMethod.POST,
                                        "/api/admin/courses/" + publishedId + "/archive",
                                        SETTER)
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private static JsonNode findBySlug(JsonNode page, String slug) {
        for (JsonNode item : page.get("items")) {
            if (item.get("slug").asText().equals(slug)) {
                return item;
            }
        }
        throw new AssertionError("Không thấy khoá " + slug);
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return objectMapper.readTree(response.getBody());
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, method, new HttpEntity<>(headers), String.class);
    }
}
