package com.sqljudge.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.notification.entity.Notification;
import com.sqljudge.notification.entity.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String link,
        @JsonProperty("isRead") boolean isRead,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getLink(),
                notification.getReadAt() != null,
                notification.getCreatedAt());
    }
}
