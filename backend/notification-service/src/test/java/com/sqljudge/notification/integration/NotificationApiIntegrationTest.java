package com.sqljudge.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.notification.event.NotificationEventConsumer;
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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class NotificationApiIntegrationTest extends ContainerSupport {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private NotificationEventConsumer consumer;

    private UUID userId;
    private String token;

    @BeforeEach
    void signIn() {
        userId = UUID.randomUUID();
        token = TestJwts.token(userId, "LEARNER");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM notifications");
    }

    @Test
    @DisplayName("Mỗi loại event tạo đúng thông báo cho đúng người, event trùng không tạo thêm")
    void events_createNotificationsOnce() throws Exception {
        UUID postId = UUID.randomUUID();
        String commented =
                """
                {"eventId": "%s", "occurredAt": "2026-11-24T08:00:00Z", "version": 1,
                 "commentId": "%s", "postId": "%s", "problemId": null,
                 "authorId": "%s", "recipientId": "%s"}
                """
                        .formatted(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                postId,
                                UUID.randomUUID(),
                                userId);
        consumer.onCommented(commented);
        consumer.onCommented(commented);
        consumer.onReportResolved(
                """
                {"eventId": "%s", "targetType": "COMMENT", "targetId": "%s",
                 "authorId": "%s", "action": "REMOVE"}
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID(), userId));
        consumer.onReportResolved(
                """
                {"eventId": "%s", "targetType": "POST", "targetId": "%s",
                 "authorId": "%s", "action": "DISMISS"}
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID(), userId));
        consumer.onProblemPublished(
                """
                {"eventId": "%s", "problemId": "%s", "number": 12, "slug": "top-earners",
                 "title": "Top lương", "difficulty": "EASY", "authorId": "%s"}
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID(), userId));
        consumer.onRatingUpdated(
                """
                {"eventId": "%s", "contestId": "%s",
                 "changes": [{"userId": "%s", "newRating": 1512, "delta": -38},
                             {"userId": "%s", "newRating": 1600, "delta": 12}]}
                """
                        .formatted(
                                UUID.randomUUID(), UUID.randomUUID(), userId, UUID.randomUUID()));

        DocumentContext feed = JsonPath.parse(get("/api/notifications").getBody());

        assertThat(feed.read("$.items[*].type", List.class))
                .containsExactly(
                        "RATING_CHANGED", "PROBLEM_PUBLISHED", "CONTENT_REMOVED", "COMMENT_REPLY");
        assertThat(feed.read("$.items[0].title", String.class))
                .isEqualTo("Rating của bạn giảm 38, hiện là 1.512");
        assertThat(feed.read("$.items[1].link", String.class)).isEqualTo("/problems/top-earners");
        assertThat(feed.read("$.items[3].link", String.class)).isEqualTo("/community/" + postId);
        assertThat(feed.read("$.items[2].title", String.class))
                .isEqualTo("Bình luận của bạn đã bị gỡ vì vi phạm quy định cộng đồng");
        assertThat(unread()).isEqualTo(4);
    }

    @Test
    @DisplayName(
            "Phân trang bằng cursor, đánh dấu đã đọc một hoặc tất cả, không đụng thông báo người"
                    + " khác")
    void feed_paginatesAndMarksRead() throws Exception {
        for (int index = 0; index < 3; index++) {
            publish(userId);
        }
        UUID stranger = UUID.randomUUID();
        publish(stranger);

        DocumentContext first = JsonPath.parse(get("/api/notifications?size=2").getBody());
        String cursor = first.read("$.nextCursor", String.class);
        DocumentContext second =
                JsonPath.parse(get("/api/notifications?size=2&cursor=" + cursor).getBody());

        assertThat(first.read("$.items", List.class)).hasSize(2);
        assertThat(second.read("$.items", List.class)).hasSize(1);
        assertThat(second.read("$.nextCursor", String.class)).isNull();

        String firstId = first.read("$.items[0].id", String.class);
        assertThat(post("/api/notifications/" + firstId + "/read", token).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(unread()).isEqualTo(2);
        assertThat(
                        post(
                                        "/api/notifications/" + firstId + "/read",
                                        TestJwts.token(stranger, "LEARNER"))
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        post("/api/notifications/read-all", token);
        assertThat(unread()).isZero();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM notifications WHERE read_at IS NULL",
                                Integer.class))
                .isEqualTo(1);
    }

    private void publish(UUID recipient) throws Exception {
        consumer.onProblemPublished(
                """
                {"eventId": "%s", "slug": "bai", "title": "Bài", "authorId": "%s"}
                """
                        .formatted(UUID.randomUUID(), recipient));
    }

    private long unread() {
        return JsonPath.parse(get("/api/notifications/unread-count").getBody())
                .read("$.count", Long.class);
    }

    private ResponseEntity<String> get(String path) {
        return exchange(HttpMethod.GET, path, token);
    }

    private ResponseEntity<String> post(String path, String bearer) {
        return exchange(HttpMethod.POST, path, bearer);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        return restTemplate.exchange(path, method, new HttpEntity<>(headers), String.class);
    }
}
