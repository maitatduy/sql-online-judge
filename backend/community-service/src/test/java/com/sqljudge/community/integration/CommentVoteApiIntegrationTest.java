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
class CommentVoteApiIntegrationTest extends ContainerSupport {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID READER_ID = UUID.randomUUID();
    private static final String AUTHOR = TestJwts.token(AUTHOR_ID, "LEARNER");
    private static final String READER = TestJwts.token(READER_ID, "LEARNER");
    private static final String MODERATOR = TestJwts.token(UUID.randomUUID(), "MODERATOR");
    private static final String COMMENTS = "/api/community/comments";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String postId;

    @DynamicPropertySource
    static void authService(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.auth-service-url", AuthServiceStub::url);
        registry.add("sqljudge.contest-service-url", ContestServiceStub::url);
    }

    @BeforeEach
    void createPost() {
        AuthServiceStub.reset();
        ContestServiceStub.reset();
        AuthServiceStub.user(AUTHOR_ID, "trang.hoang");
        AuthServiceStub.user(READER_ID, "quang.le");
        String body =
                "{\"category\": \"SOLUTION\", \"title\": \"Dùng DENSE_RANK cho bài #184\","
                        + " \"bodyMarkdown\": \"Nội dung\", \"tags\": [], \"publish\": true}";
        postId =
                JsonPath.parse(
                                send(HttpMethod.POST, "/api/community/posts", body, AUTHOR)
                                        .getBody())
                        .read("$.id", String.class);
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM votes");
        jdbcTemplate.update("DELETE FROM bookmarks");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM posts");
    }

    @Test
    @DisplayName(
            "Bình luận và trả lời hiện lồng nhau, tăng số bình luận, báo cho người nhận qua outbox")
    void commentsAndReplies_areNestedAndAnnounced() {
        String rootId =
                comment(
                        "{\"postId\": \""
                                + postId
                                + "\", \"body\": \"MySQL 5.7 chưa có hàm cửa sổ.\"}",
                        READER);
        comment(
                "{\"postId\": \""
                        + postId
                        + "\", \"parentId\": \""
                        + rootId
                        + "\", \"body\": \"Chuẩn.\"}",
                AUTHOR);

        DocumentContext page = JsonPath.parse(get(COMMENTS + "?postId=" + postId, null).getBody());

        assertThat(page.read("$.items[*].id", List.class)).containsExactly(rootId);
        assertThat(page.read("$.items[0].authorUsername", String.class)).isEqualTo("quang.le");
        assertThat(page.read("$.items[0].replies[0].body", String.class)).isEqualTo("Chuẩn.");
        assertThat(postField("$.commentCount", AUTHOR)).isEqualTo(2);
        assertThat(
                        jdbcTemplate.queryForList(
                                "SELECT payload FROM outbox_events WHERE topic = ?",
                                String.class,
                                "sqljudge.community.commented"))
                .hasSize(2)
                .anySatisfy(payload -> assertThat(payload).contains(AUTHOR_ID.toString()))
                .anySatisfy(payload -> assertThat(payload).contains(READER_ID.toString()));
    }

    @Test
    @DisplayName(
            "Trả lời một câu trả lời thì nằm cùng nhóm dưới bình luận gốc và báo cho người được"
                    + " trả lời")
    void replyToReply_joinsRootThreadAndNotifiesReplyAuthor() {
        String rootId = comment("{\"postId\": \"" + postId + "\", \"body\": \"Gốc\"}", READER);
        String replyId =
                comment(
                        "{\"postId\": \""
                                + postId
                                + "\", \"parentId\": \""
                                + rootId
                                + "\", \"body\": \"Trả lời\"}",
                        AUTHOR);
        jdbcTemplate.update("DELETE FROM outbox_events");

        comment(
                "{\"postId\": \""
                        + postId
                        + "\", \"parentId\": \""
                        + replyId
                        + "\", \"body\": \"@trang.hoang đồng ý\"}",
                READER);

        DocumentContext page = JsonPath.parse(get(COMMENTS + "?postId=" + postId, null).getBody());
        assertThat(page.read("$.items[*].id", List.class)).containsExactly(rootId);
        assertThat(page.read("$.items[0].replies[*].body", List.class))
                .containsExactly("Trả lời", "@trang.hoang đồng ý");
        assertThat(page.read("$.items[0].replies[1].parentId", String.class)).isEqualTo(rootId);
        assertThat(
                        jdbcTemplate.queryForList(
                                "SELECT payload FROM outbox_events WHERE topic = ?",
                                String.class,
                                "sqljudge.community.commented"))
                .singleElement()
                .satisfies(payload -> assertThat(payload).contains(AUTHOR_ID.toString()));
    }

    @Test
    @DisplayName("Phải gắn đúng một chủ đề, thảo luận theo bài tập đọc được")
    void commentTargets_areValidated() {
        String problemId = UUID.randomUUID().toString();

        assertThat(code(send(HttpMethod.POST, COMMENTS, "{\"body\": \"x\"}", READER)))
                .isEqualTo("INVALID_COMMENT_TARGET");
        comment(
                "{\"problemId\": \"" + problemId + "\", \"body\": \"Testcase 6 có tên trùng.\"}",
                READER);
        assertThat(
                        JsonPath.parse(get(COMMENTS + "?problemId=" + problemId, null).getBody())
                                .read("$.items[*].body", List.class))
                .containsExactly("Testcase 6 có tên trùng.");
        assertThat(
                        send(
                                        HttpMethod.POST,
                                        COMMENTS,
                                        "{\"problemId\": \"" + problemId + "\", \"body\": \"x\"}",
                                        null)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Vote lên, đổi chiều, bỏ vote cập nhật đúng điểm; người nhận vote vào top tuần")
    void votes_updateScoreAndContributors() {
        assertThat(vote("POST", postId, 1, READER)).isEqualTo(1);
        assertThat(vote("POST", postId, -1, READER)).isEqualTo(-1);
        assertThat(vote("POST", postId, 0, READER)).isZero();
        String rootId = comment("{\"postId\": \"" + postId + "\", \"body\": \"Hay\"}", READER);
        assertThat(vote("COMMENT", rootId, 1, AUTHOR)).isEqualTo(1);
        vote("POST", postId, 1, READER);

        assertThat(postField("$.myVote", READER)).isEqualTo(1);
        DocumentContext contributors =
                JsonPath.parse(get("/api/community/contributors", null).getBody());
        assertThat(contributors.read("$[*].username", List.class))
                .containsExactlyInAnyOrder("trang.hoang", "quang.le");
        assertThat(
                        JsonPath.parse(get(COMMENTS + "?postId=" + postId, AUTHOR).getBody())
                                .read("$.items[0].myVote", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Bình luận chỉ nhận thả tim hoặc bỏ tim, vote xuống trả 400 và không đổi số tim")
    void commentVote_downvoteRejected() {
        String rootId = comment("{\"postId\": \"" + postId + "\", \"body\": \"Hay\"}", READER);
        vote("COMMENT", rootId, 1, AUTHOR);

        ResponseEntity<String> downvote =
                send(
                        HttpMethod.PUT,
                        "/api/community/votes",
                        "{\"targetType\": \"COMMENT\", \"targetId\": \""
                                + rootId
                                + "\", \"value\": -1}",
                        AUTHOR);

        assertThat(downvote.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(downvote.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("value");
        assertThat(
                        JsonPath.parse(get(COMMENTS + "?postId=" + postId, AUTHOR).getBody())
                                .read("$.items[0].voteScore", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Lưu bài thì hiện ở tab Đã lưu, bỏ lưu thì biến mất")
    void bookmarks_feedSavedTab() {
        assertThat(
                        send(
                                        HttpMethod.PUT,
                                        "/api/community/posts/" + postId + "/bookmark",
                                        null,
                                        READER)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(savedIds(READER)).containsExactly(postId);
        assertThat(savedIds(AUTHOR)).isEmpty();
        assertThat(
                        JsonPath.parse(get("/api/community/posts/" + postId, READER).getBody())
                                .read("$.isBookmarked", Boolean.class))
                .isTrue();

        send(HttpMethod.DELETE, "/api/community/posts/" + postId + "/bookmark", null, READER);
        assertThat(savedIds(READER)).isEmpty();
    }

    @Test
    @DisplayName(
            "Người khác không xoá được bình luận, kiểm duyệt viên xoá được và số bình luận giảm")
    void deleteComment_permissions() {
        String rootId = comment("{\"postId\": \"" + postId + "\", \"body\": \"Spam\"}", READER);

        assertThat(send(HttpMethod.DELETE, COMMENTS + "/" + rootId, null, AUTHOR).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(
                        send(HttpMethod.DELETE, COMMENTS + "/" + rootId, null, MODERATOR)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(postField("$.commentCount", AUTHOR)).isZero();
        assertThat(
                        JsonPath.parse(get(COMMENTS + "?postId=" + postId, null).getBody())
                                .read("$.items", List.class))
                .isEmpty();
    }

    private List<String> savedIds(String token) {
        return JsonPath.parse(get("/api/community/posts?tab=SAVED", token).getBody())
                .read("$.items[*].id");
    }

    private int vote(String type, String targetId, int value, String token) {
        ResponseEntity<String> response =
                send(
                        HttpMethod.PUT,
                        "/api/community/votes",
                        "{\"targetType\": \""
                                + type
                                + "\", \"targetId\": \""
                                + targetId
                                + "\", \"value\": "
                                + value
                                + "}",
                        token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody()).read("$.voteScore", Integer.class);
    }

    private String comment(String body, String token) {
        ResponseEntity<String> created = send(HttpMethod.POST, COMMENTS, body, token);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String location = created.getHeaders().getLocation().getPath();
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private int postField(String path, String token) {
        return JsonPath.parse(get("/api/community/posts/" + postId, token).getBody())
                .read(path, Integer.class);
    }

    private static String code(ResponseEntity<String> response) {
        return JsonPath.parse(response.getBody()).read("$.code", String.class);
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
