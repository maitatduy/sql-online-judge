package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
class StudioProblemApiIntegrationTest extends ContainerSupport {

    private static final String PATH = "/api/studio/problems";
    private static final UUID SETTER_ID = UUID.fromString("0192f3a0-0000-7000-8000-0000000000c1");
    private static final UUID OTHER_SETTER_ID =
            UUID.fromString("0192f3a0-0000-7000-8000-0000000000c2");
    private static final String SETTER = TestJwts.token(SETTER_ID, "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(OTHER_SETTER_ID, "SETTER");

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteProblems() {
        jdbcTemplate.update("UPDATE problems SET draft_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
    }

    @Test
    @DisplayName("Người ra đề tạo bài nháp: 201, Location, slug bỏ dấu sinh từ tiêu đề")
    void create_setter_returnsCreatedDraft() {
        ResponseEntity<String> response = create(SETTER, "Lương trung vị mỗi phòng ban");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DocumentContext body = JsonPath.parse(response.getBody());
        String id = body.read("$.id", String.class);
        assertThat(response.getHeaders().getLocation()).hasToString(PATH + "/" + id);
        assertThat(body.read("$.slug", String.class)).isEqualTo("luong-trung-vi-moi-phong-ban");
        assertThat(body.read("$.title", String.class)).isEqualTo("Lương trung vị mỗi phòng ban");
        assertThat(body.read("$.status", String.class)).isEqualTo("DRAFT");
        assertThat(body.read("$.number", Object.class)).isNull();
        assertThat(body.read("$.updatedAt", String.class)).isNotBlank();
    }

    @Test
    @DisplayName("Tiêu đề trùng bài khác thì slug thêm hậu tố số, ký tự đặc biệt bị bỏ")
    void create_duplicateTitle_appendsNumberToSlug() {
        create(SETTER, "Đơn hàng đã giao (2026)");

        ResponseEntity<String> second = create(OTHER_SETTER, "Đơn hàng đã giao (2026)");

        assertThat(JsonPath.parse(second.getBody()).read("$.slug", String.class))
                .isEqualTo("don-hang-da-giao-2026-2");
    }

    @Test
    @DisplayName("Tiêu đề trống hoặc dài quá 200 ký tự trả 400 kèm lỗi ở trường title")
    void create_invalidTitle_returnsValidationError() {
        for (String title : List.of("   ", "a".repeat(201))) {
            ResponseEntity<String> response = create(SETTER, title);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(JsonPath.parse(response.getBody()).read("$.errors[0].field", String.class))
                    .isEqualTo("title");
        }
    }

    @Test
    @DisplayName("Học viên không được tạo bài, khách chưa đăng nhập bị từ chối")
    void create_learnerOrAnonymous_isRejected() {
        assertThat(create(TestJwts.learnerToken(), "Bài thử").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(create(null, "Bài thử").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get(PATH, TestJwts.learnerToken()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Bài của tôi chỉ có bài của chính mình, mới sửa nhất trước, lọc được")
    void list_returnsOnlyOwnProblemsWithFilters() {
        create(SETTER, "Phễu chuyển đổi 4 bước");
        create(SETTER, "Lương trung vị mỗi phòng ban");
        create(OTHER_SETTER, "Bài của người khác");
        jdbcTemplate.update(
                "UPDATE problems SET status = 'PENDING_REVIEW',"
                        + " updated_at = NOW(6) + INTERVAL 1 MINUTE WHERE title = ?",
                "Phễu chuyển đổi 4 bước");

        DocumentContext all = JsonPath.parse(get(PATH, SETTER).getBody());
        DocumentContext drafts = JsonPath.parse(get(PATH + "?status=DRAFT", SETTER).getBody());
        DocumentContext searched = JsonPath.parse(get(PATH + "?q=pheu", SETTER).getBody());

        assertThat(all.read("$.items[*].title", List.class))
                .containsExactly("Phễu chuyển đổi 4 bước", "Lương trung vị mỗi phòng ban");
        assertThat(all.read("$.totalItems", Integer.class)).isEqualTo(2);
        assertThat(drafts.read("$.items[*].title", List.class))
                .containsExactly("Lương trung vị mỗi phòng ban");
        assertThat(searched.read("$.items[*].title", List.class))
                .containsExactly("Phễu chuyển đổi 4 bước");
    }

    @Test
    @DisplayName("Đếm bài của tôi theo trạng thái, trạng thái chưa có bài đếm là 0")
    void statusCounts_countsOwnProblemsByStatus() {
        create(SETTER, "Bài một");
        create(SETTER, "Bài hai");
        create(OTHER_SETTER, "Bài của người khác");

        Map<String, Integer> counts =
                JsonPath.parse(get(PATH + "/status-counts", SETTER).getBody()).read("$", Map.class);

        assertThat(counts)
                .containsEntry("DRAFT", 2)
                .containsEntry("PENDING_REVIEW", 0)
                .containsEntry("CHANGES_REQUESTED", 0)
                .containsEntry("REJECTED", 0)
                .containsEntry("PUBLISHED", 0);
    }

    private ResponseEntity<String> create(String token, String title) {
        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"title\":\"" + title + "\"}";
        return restTemplate.exchange(
                PATH, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> get(String path, String token) {
        return restTemplate.exchange(
                path, HttpMethod.GET, new HttpEntity<>(headers(token)), String.class);
    }

    private static HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }
}
