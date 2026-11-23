package com.sqljudge.community.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class PostApiIntegrationTest extends ContainerSupport {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final String AUTHOR = TestJwts.token(AUTHOR_ID, "LEARNER");
    private static final String OTHER = TestJwts.token(UUID.randomUUID(), "LEARNER");
    private static final String MODERATOR = TestJwts.token(UUID.randomUUID(), "MODERATOR");
    private static final String POSTS = "/api/community/posts";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void authService(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.auth-service-url", AuthServiceStub::url);
        registry.add("sqljudge.contest-service-url", ContestServiceStub::url);
        registry.add("sqljudge.problem-service-url", ProblemServiceStub::url);
    }

    @BeforeEach
    void stubAuthors() {
        AuthServiceStub.reset();
        ContestServiceStub.reset();
        ProblemServiceStub.reset();
        AuthServiceStub.user(AUTHOR_ID, "trang.hoang");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM posts");
    }

    @Test
    @DisplayName("Đăng bài thì hiện trong danh sách kèm tác giả, trích đoạn và tag")
    void publish_showsPostInList() {
        String id = create("Dùng DENSE_RANK cho bài #184", "SOLUTION", true);

        DocumentContext page = JsonPath.parse(get(POSTS, null).getBody());

        assertThat(page.read("$.items[*].id", List.class)).containsExactly(id);
        assertThat(page.read("$.items[0].authorUsername", String.class)).isEqualTo("trang.hoang");
        assertThat(page.read("$.items[0].excerpt", String.class))
                .isEqualTo("Ý tưởng Dùng hàm cửa sổ để quét bảng một lần.");
        assertThat(page.read("$.items[0].tags", List.class))
                .containsExactly("postgresql", "window-function");
        DocumentContext overview = JsonPath.parse(get("/api/community/overview", null).getBody());
        assertThat(overview.read("$.categoryCounts.SOLUTION", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Lọc theo chuyên mục, tag, từ khoá và tab chưa có trả lời")
    void list_filters() {
        create("Vì sao COUNT(*) khác COUNT(cột)?", "QUESTION", true);
        create("Phỏng vấn Data Analyst ở ba công ty", "INTERVIEW", true);

        assertThat(titles("?category=QUESTION"))
                .containsExactly("Vì sao COUNT(*) khác COUNT(cột)?");
        assertThat(titles("?q=phỏng vấn")).containsExactly("Phỏng vấn Data Analyst ở ba công ty");
        assertThat(titles("?tag=postgresql")).hasSize(2);
        assertThat(titles("?tab=UNANSWERED")).hasSize(2);
    }

    @Test
    @DisplayName("Bản nháp không có trong danh sách, chỉ tác giả xem được; đăng bản nháp thì hiện")
    void draft_isVisibleOnlyToAuthorUntilPublished() {
        String id = create("Bản nháp lời giải bài #203", "SOLUTION", false);

        assertThat(titles("")).isEmpty();
        assertThat(get(POSTS + "/" + id, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get(POSTS + "/" + id, OTHER).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        JsonPath.parse(get(POSTS + "/" + id, AUTHOR).getBody())
                                .read("$.status", String.class))
                .isEqualTo("DRAFT");

        send(HttpMethod.PUT, POSTS + "/" + id, body("Lời giải bài #203", "SOLUTION", true), AUTHOR);

        assertThat(titles("")).containsExactly("Lời giải bài #203");
    }

    @Test
    @DisplayName("Chỉ tác giả được sửa; tác giả hoặc kiểm duyệt viên được xoá")
    void updateAndDelete_permissions() {
        String id = create("Bài của tác giả", "BLOG", true);

        assertThat(
                        send(
                                        HttpMethod.PUT,
                                        POSTS + "/" + id,
                                        body("Sửa trộm", "BLOG", true),
                                        OTHER)
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.DELETE, POSTS + "/" + id, null, OTHER).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.DELETE, POSTS + "/" + id, null, MODERATOR).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(POSTS + "/" + id, AUTHOR).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Dữ liệu sai trả 400, chưa đăng nhập trả 401")
    void create_validatesAndRequiresLogin() {
        String tooManyTags =
                "{\"category\": \"BLOG\", \"title\": \"Tiêu đề đủ dài\", \"bodyMarkdown\": \"x\","
                    + " \"tags\": [\"a\", \"b\", \"c\", \"d\", \"e\", \"f\"], \"publish\": true}";
        String badTag =
                "{\"category\": \"BLOG\", \"title\": \"Tiêu đề đủ dài\", \"bodyMarkdown\": \"x\","
                        + " \"tags\": [\"Sai Tag\"], \"publish\": true}";

        assertThat(send(HttpMethod.POST, POSTS, tooManyTags, AUTHOR).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(send(HttpMethod.POST, POSTS, badTag, AUTHOR).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(
                        send(HttpMethod.POST, POSTS, body("Tiêu đề", "BLOG", true), null)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Gắn bài tập thì lưu slug và tên bài; bài không tồn tại thì báo 404")
    void linkProblem_storesSnapshot() {
        UUID problemId = UUID.randomUUID();
        String body =
                "{\"category\": \"SOLUTION\", \"title\": \"Lời giải bài lương\", \"bodyMarkdown\":"
                        + " \"SELECT 1\", \"problemId\": \"%s\", \"tags\": [], \"publish\": true}";

        ResponseEntity<String> created =
                send(HttpMethod.POST, POSTS, body.formatted(problemId), AUTHOR);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DocumentContext post = JsonPath.parse(created.getBody());
        assertThat(post.read("$.problemSlug", String.class)).isEqualTo("second-highest-salary");
        assertThat(post.read("$.problemTitle", String.class)).isEqualTo("Lương cao thứ hai");
        UUID missing = UUID.randomUUID();
        ProblemServiceStub.missing(missing);
        assertThat(send(HttpMethod.POST, POSTS, body.formatted(missing), AUTHOR).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String create(String title, String category, boolean publish) {
        ResponseEntity<String> created =
                send(HttpMethod.POST, POSTS, body(title, category, publish), AUTHOR);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return JsonPath.parse(created.getBody()).read("$.id", String.class);
    }

    private List<String> titles(String query) {
        return JsonPath.parse(get(POSTS + query, null).getBody()).read("$.items[*].title");
    }

    private static String body(String title, String category, boolean publish) {
        return """
        {"category": "%s", "title": "%s",
         "bodyMarkdown": "## Ý tưởng\\n\\nDùng **hàm cửa sổ** để quét bảng một lần.\\n\\n```sql\\nSELECT 1;\\n```",
         "tags": ["postgresql", "window-function"], "publish": %s}
        """
                .formatted(category, title, publish);
    }

    private ResponseEntity<String> get(String path, String token) {
        return send(HttpMethod.GET, path, null, token);
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
