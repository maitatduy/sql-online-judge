package com.sqljudge.auth.event;

import java.util.UUID;

public record UnbanNotificationEmailRequestedEvent(UUID userId, String email, String username) {

    @Override
    public String toString() {
        return "UnbanNotificationEmailRequestedEvent[userId=" + userId + "]";
    }
}
