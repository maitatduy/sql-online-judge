package com.sqljudge.community.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.community.service.ContestUnlockService;
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
class ReportApiIntegrationTest extends ContainerSupport {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final String AUTHOR = TestJwts.token(AUTHOR_ID, "LEARNER");
    private static final String MODERATOR = TestJwts.token(UUID.randomUUID(), "MODERATOR");
    private static final String REPORTS = "/api/community/reports";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ContestUnlockService contestUnlockService;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.auth-service-url", AuthServiceStub::url);
        registry.add("sqljudge.contest-service-url", ContestServiceStub::url);
        registry.add("sqljudge.problem-service-url", ProblemServiceStub::url);
    }

    @BeforeEach
    void stubServices() {
        AuthServiceStub.reset();
        ContestServiceStub.reset();
        AuthServiceStub.user(AUTHOR_ID, "viet.luu");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM reports");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM posts");
    }

    @Test
    @DisplayName("Ba người báo cáo thì bài tự ẩn, admin thấy vụ báo cáo gom theo nội dung")
    void threeReports_hidePostAndShowOneCase() {
        String postId = post(null);

        for (int index = 0; index < 3; index++) {
            assertThat(report("POST", postId, "CONTEST_SPOILER", learner()).getStatusCode())
                    .isEqualTo(HttpStatus.CREATED);
        }

        assertThat(listedPostIds()).isEmpty();
        assertThat(
                        send(HttpMethod.GET, "/api/community/posts/" + postId, null, AUTHOR)
                                .getStatusCode())
                .isEqualTo(HttpStatus.OK);
        DocumentContext cases =
                JsonPath.parse(
                        send(HttpMethod.GET, "/api/admin/reports", null, MODERATOR).getBody());
        assertThat(cases.read("$.items[*].targetId", List.class)).containsExactly(postId);
        assertThat(cases.read("$.items[0].reportCount", Integer.class)).isEqualTo(3);
        assertThat(cases.read("$.items[0].reasons.CONTEST_SPOILER", Integer.class)).isEqualTo(3);
        assertThat(cases.read("$.items[0].authorUsername", String.class)).isEqualTo("viet.luu");
    }

    @Test
    @DisplayName("Không báo cáo trùng; bỏ qua thì hiện lại nội dung và báo cho tác giả qua outbox")
    void dismiss_restoresContent() {
        String postId = post(null);
        String reporter = learner();
        report("POST", postId, "SPAM", reporter);
        assertThat(code(report("POST", postId, "SPAM", reporter))).isEqualTo("ALREADY_REPORTED");
        report("POST", postId, "SPAM", learner());
        report("POST", postId, "SPAM", learner());

        assertThat(resolve("POST", postId, "DISMISS").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(listedPostIds()).containsExactly(postId);
        assertThat(
                        JsonPath.parse(
                                        send(HttpMethod.GET, "/api/admin/reports", null, MODERATOR)
                                                .getBody())
                                .read("$.items", List.class))
                .isEmpty();
        assertThat(outboxCount("sqljudge.community.report-resolved")).isEqualTo(1);
        assertThat(resolve("POST", postId, "DISMISS").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Gỡ bình luận vi phạm thì bình luận biến mất và tác giả có thêm một vi phạm")
    void removeComment_deletesItAndCountsViolation() {
        String postId = post(null);
        ResponseEntity<String> created =
                send(
                        HttpMethod.POST,
                        "/api/community/comments",
                        "{\"postId\": \""
                                + postId
                                + "\", \"body\": \"ai bí bài B thì dùng cái này\"}",
                        AUTHOR);
        String location = created.getHeaders().getLocation().getPath();
        String commentId = location.substring(location.lastIndexOf('/') + 1);
        report("COMMENT", commentId, "CONTEST_SPOILER", learner());

        DocumentContext detail =
                JsonPath.parse(
                        send(
                                        HttpMethod.GET,
                                        "/api/admin/reports/COMMENT/" + commentId,
                                        null,
                                        MODERATOR)
                                .getBody());
        assertThat(detail.read("$.content.body", String.class))
                .isEqualTo("ai bí bài B thì dùng cái này");
        assertThat(detail.read("$.reports", List.class)).hasSize(1);
        assertThat(detail.read("$.priorViolationCount", Integer.class)).isZero();

        resolve("COMMENT", commentId, "REMOVE");

        assertThat(
                        JsonPath.parse(
                                        send(
                                                        HttpMethod.GET,
                                                        "/api/community/comments?postId=" + postId,
                                                        null,
                                                        null)
                                                .getBody())
                                .read("$.items", List.class))
                .isEmpty();
        assertThat(
                        JsonPath.parse(
                                        send(
                                                        HttpMethod.GET,
                                                        "/api/admin/reports/COMMENT/" + commentId,
                                                        null,
                                                        MODERATOR)
                                                .getBody())
                                .read("$.priorViolationCount", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Bài gắn bài tập đang thi tự ẩn đến khi contest kết thúc; contest-service lỗi thì 503")
    void contestProblem_hidesUntilContestFinishes() {
        UUID problemId = UUID.randomUUID();
        ContestServiceStub.inUnfinishedContest(problemId);

        String postId = post(problemId);
        send(
                HttpMethod.POST,
                "/api/community/comments",
                "{\"problemId\": \"" + problemId + "\", \"body\": \"Gợi ý bài B\"}",
                AUTHOR);

        assertThat(listedPostIds()).isEmpty();
        assertThat(commentsOf(problemId)).isEmpty();

        contestUnlockService.unlock(List.of(problemId));

        assertThat(listedPostIds()).containsExactly(postId);
        assertThat(commentsOf(problemId)).hasSize(1);
        ContestServiceStub.failWith(503);
        assertThat(
                        send(HttpMethod.POST, "/api/community/posts", postBody(problemId), AUTHOR)
                                .getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Học viên không xem được báo cáo, khách không báo cáo được")
    void permissions() {
        assertThat(send(HttpMethod.GET, "/api/admin/reports", null, AUTHOR).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(report("USER", UUID.randomUUID().toString(), "SPAM", null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String post(UUID problemId) {
        ResponseEntity<String> created =
                send(HttpMethod.POST, "/api/community/posts", postBody(problemId), AUTHOR);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return JsonPath.parse(created.getBody()).read("$.id", String.class);
    }

    private static String postBody(UUID problemId) {
        String problem = problemId == null ? "null" : "\"" + problemId + "\"";
        return "{\"category\": \"SOLUTION\", \"title\": \"Lời giải bài B\", \"bodyMarkdown\":"
                + " \"SELECT 1\", \"problemId\": "
                + problem
                + ", \"tags\": [], \"publish\": true}";
    }

    private List<String> listedPostIds() {
        return JsonPath.parse(send(HttpMethod.GET, "/api/community/posts", null, null).getBody())
                .read("$.items[*].id");
    }

    private List<Object> commentsOf(UUID problemId) {
        return JsonPath.parse(
                        send(
                                        HttpMethod.GET,
                                        "/api/community/comments?problemId=" + problemId,
                                        null,
                                        null)
                                .getBody())
                .read("$.items");
    }

    private ResponseEntity<String> report(
            String type, String targetId, String reason, String token) {
        return send(
                HttpMethod.POST,
                REPORTS,
                "{\"targetType\": \""
                        + type
                        + "\", \"targetId\": \""
                        + targetId
                        + "\", \"reason\": \""
                        + reason
                        + "\"}",
                token);
    }

    private ResponseEntity<String> resolve(String type, String targetId, String action) {
        return send(
                HttpMethod.POST,
                "/api/admin/reports/" + type + "/" + targetId + "/resolve",
                "{\"action\": \"" + action + "\", \"note\": \"Đã xem\"}",
                MODERATOR);
    }

    private int outboxCount(String topic) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE topic = ?", Integer.class, topic);
    }

    private static String learner() {
        return TestJwts.token(UUID.randomUUID(), "LEARNER");
    }

    private static String code(ResponseEntity<String> response) {
        return JsonPath.parse(response.getBody()).read("$.code", String.class);
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
