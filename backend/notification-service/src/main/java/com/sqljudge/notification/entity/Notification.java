package com.sqljudge.notification.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "notifications",
        indexes = {
            @Index(
                    name = "uk_notifications_event_id_user_id",
                    columnList = "event_id, user_id",
                    unique = true),
            @Index(name = "idx_notifications_user_id_id", columnList = "user_id, id"),
            @Index(name = "idx_notifications_user_id_read_at", columnList = "user_id, read_at")
        })
@Getter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "event_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "link", length = 300)
    private String link;

    @Column(name = "read_at")
    private Instant readAt;

    public static Notification of(NewNotification draft) {
        Notification notification = new Notification();
        notification.userId = draft.userId();
        notification.eventId = draft.eventId();
        notification.type = draft.type();
        notification.title = draft.title();
        notification.link = draft.link();
        return notification;
    }

    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public record NewNotification(
            UUID userId, UUID eventId, NotificationType type, String title, String link) {}
}
