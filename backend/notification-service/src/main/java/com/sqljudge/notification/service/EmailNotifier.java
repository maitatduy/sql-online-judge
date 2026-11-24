package com.sqljudge.notification.service;

import com.sqljudge.notification.client.AuthServiceClient;
import com.sqljudge.notification.client.UserContact;
import com.sqljudge.notification.config.EmailProperties;
import com.sqljudge.notification.entity.NotificationPreference;
import com.sqljudge.notification.repository.NotificationPreferenceRepository;
import com.sqljudge.notification.service.NotificationsDelivered.Delivered;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotifier {

    private static final String SUBJECT_PREFIX = "SQL Judge: ";

    private final NotificationPreferenceRepository preferenceRepository;
    private final AuthServiceClient authServiceClient;
    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    @Async
    @TransactionalEventListener
    public void onDelivered(NotificationsDelivered event) {
        event.notifications().stream()
                .filter(notification -> notification.type().isEmailed())
                .filter(this::wantsEmail)
                .forEach(this::send);
    }

    private boolean wantsEmail(Delivered notification) {
        return preferenceRepository
                .findByUserId(notification.userId())
                .map(NotificationPreference::isEmailEnabled)
                .orElse(true);
    }

    private void send(Delivered notification) {
        try {
            UserContact contact = authServiceClient.contact(notification.userId());
            if (!contact.isEmailVerified()) {
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailProperties.from());
            message.setTo(contact.email());
            message.setSubject(SUBJECT_PREFIX + notification.title());
            message.setText(bodyOf(contact.username(), notification));
            mailSender.send(message);
            log.info(
                    "notification_email_sent userId={} type={}",
                    notification.userId(),
                    notification.type());
        } catch (RuntimeException error) {
            log.warn(
                    "notification_email_failed userId={} type={}",
                    notification.userId(),
                    notification.type(),
                    error);
        }
    }

    private String bodyOf(String username, Delivered notification) {
        String link =
                notification.link() == null
                        ? emailProperties.frontendUrl() + "/notifications"
                        : emailProperties.frontendUrl() + notification.link();
        return """
        Chào %s,

        %s

        Xem chi tiết: %s

        Bạn có thể tắt email thông báo trong trang Thông báo của SQL Judge.
        """
                .formatted(username, notification.title(), link);
    }
}
