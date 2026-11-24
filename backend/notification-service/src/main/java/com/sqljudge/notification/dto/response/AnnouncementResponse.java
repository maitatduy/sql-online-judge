package com.sqljudge.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.notification.entity.Announcement;
import java.time.Instant;
import java.util.UUID;

public record AnnouncementResponse(
        UUID id,
        String title,
        String link,
        int recipientCount,
        @JsonProperty("isDelivered") boolean isDelivered,
        Instant createdAt) {

    public static AnnouncementResponse from(Announcement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getLink(),
                announcement.getRecipientCount(),
                announcement.isDelivered(),
                announcement.getCreatedAt());
    }
}
