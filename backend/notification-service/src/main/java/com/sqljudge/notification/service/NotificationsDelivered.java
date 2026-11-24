package com.sqljudge.notification.service;

import com.sqljudge.notification.entity.NotificationType;
import java.util.List;
import java.util.UUID;

public record NotificationsDelivered(List<Delivered> notifications) {

    public NotificationsDelivered {
        notifications = List.copyOf(notifications);
    }

    public record Delivered(UUID userId, NotificationType type, String title, String link) {}
}
