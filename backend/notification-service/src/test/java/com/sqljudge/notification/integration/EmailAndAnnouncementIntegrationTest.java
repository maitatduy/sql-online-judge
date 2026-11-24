package com.sqljudge.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.jayway.jsonpath.JsonPath;
import com.sqljudge.notification.event.NotificationEventConsumer;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class EmailAndAnnouncementIntegrationTest extends ContainerSupport {

    private static final long ASYNC_WAIT_MS = 5_000;
    private static final long QUIET_WAIT_MS = 1_000;
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private NotificationEventConsumer consumer;
    @MockitoBean private JavaMailSender mailSender;

    @DynamicPropertySource
    static void authService(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.auth-service-url", AuthServiceStub::url);
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM notification_preferences");
        jdbcTemplate.update("DELETE FROM announcements");
    }

    @Test
    @DisplayName("Thông báo quan trọng gửi email kèm link; tắt email thì không gửi")
    void importantNotification_sendsEmailUnlessDisabled() throws Exception {
        UUID userId = UUID.randomUUID();

        consumer.onProblemPublished(published(userId));

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(ASYNC_WAIT_MS)).send(sent.capture());
        assertThat(sent.getValue().getTo()).containsExactly("hoc.vien@gmail.com");
        assertThat(sent.getValue().getSubject())
                .isEqualTo("SQL Judge: Bài \"Top lương\" đã được duyệt và xuất bản");
        assertThat(sent.getValue().getText())
                .contains("http://localhost:3000/problems/top-earners");

        String token = TestJwts.token(userId, "LEARNER");
        ResponseEntity<String> updated =
                send(
                        HttpMethod.PUT,
                        "/api/notifications/preferences",
                        "{\"isEmailEnabled\": false}",
                        token);
        assertThat(JsonPath.parse(updated.getBody()).read("$.isEmailEnabled", Boolean.class))
                .isFalse();
        consumer.onProblemPublished(published(userId));

        verify(mailSender, after(QUIET_WAIT_MS).times(1)).send(any(SimpleMailMessage.class));
        String preferences =
                send(HttpMethod.GET, "/api/notifications/preferences", null, token).getBody();
        assertThat(JsonPath.parse(preferences).read("$.isEmailEnabled", Boolean.class)).isFalse();
    }

    @Test
    @DisplayName("Admin gửi thông báo hệ thống tới mọi người theo từng lô, không gửi email")
    void announcement_isDeliveredToEveryActiveUser() {
        List<UUID> users = Stream.generate(UUID::randomUUID).limit(3).toList();
        AuthServiceStub.activeUsers(users);

        ResponseEntity<String> accepted =
                send(
                        HttpMethod.POST,
                        "/api/admin/announcements",
                        "{\"title\": \"Bảo trì máy chấm lúc 22 giờ\", \"link\": \"/contests\"}",
                        ADMIN);

        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(mailSender, after(QUIET_WAIT_MS).never()).send(any(SimpleMailMessage.class));
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM notifications WHERE type = 'ANNOUNCEMENT'",
                                Integer.class))
                .isEqualTo(3);
        String history = send(HttpMethod.GET, "/api/admin/announcements", null, ADMIN).getBody();
        assertThat(JsonPath.parse(history).read("$.items[0].recipientCount", Integer.class))
                .isEqualTo(3);
        assertThat(JsonPath.parse(history).read("$.items[0].isDelivered", Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("Học viên không gửi được thông báo hệ thống; đường dẫn phải bắt đầu bằng /")
    void announcement_isGuarded() {
        String learner = TestJwts.token(UUID.randomUUID(), "LEARNER");

        assertThat(
                        send(
                                        HttpMethod.POST,
                                        "/api/admin/announcements",
                                        "{\"title\": \"x\"}",
                                        learner)
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(
                        send(
                                        HttpMethod.POST,
                                        "/api/admin/announcements",
                                        "{\"title\": \"x\", \"link\": \"https://evil.example\"}",
                                        ADMIN)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    private static String published(UUID authorId) {
        return """
        {"eventId": "%s", "slug": "top-earners", "title": "Top lương", "authorId": "%s"}
        """
                .formatted(UUID.randomUUID(), authorId);
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
