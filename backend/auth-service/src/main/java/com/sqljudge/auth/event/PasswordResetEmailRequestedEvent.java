package com.sqljudge.auth.event;

import java.util.UUID;

public record PasswordResetEmailRequestedEvent(
        UUID userId, String email, String username, String token) {

    @Override
    public String toString() {
        return "PasswordResetEmailRequestedEvent[userId=" + userId + "]";
    }
}
