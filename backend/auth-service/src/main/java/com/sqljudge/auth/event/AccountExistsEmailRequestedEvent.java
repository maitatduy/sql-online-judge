package com.sqljudge.auth.event;

import java.util.UUID;

public record AccountExistsEmailRequestedEvent(UUID userId, String email, String username) {

    @Override
    public String toString() {
        return "AccountExistsEmailRequestedEvent[userId=" + userId + "]";
    }
}
