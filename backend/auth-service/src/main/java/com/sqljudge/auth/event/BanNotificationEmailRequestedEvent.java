package com.sqljudge.auth.event;

import java.time.Instant;
import java.util.UUID;

public record BanNotificationEmailRequestedEvent(
        UUID userId, String email, String username, Instant bannedUntil, String reason) {

    @Override
    public String toString() {
        return "BanNotificationEmailRequestedEvent[userId=" + userId + "]";
    }
}
