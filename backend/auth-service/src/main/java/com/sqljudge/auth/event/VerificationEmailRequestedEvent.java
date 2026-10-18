package com.sqljudge.auth.event;

import java.util.UUID;

public record VerificationEmailRequestedEvent(
        UUID userId, String email, String username, String token, String code) {

    @Override
    public String toString() {
        return "VerificationEmailRequestedEvent[userId=" + userId + "]";
    }
}
