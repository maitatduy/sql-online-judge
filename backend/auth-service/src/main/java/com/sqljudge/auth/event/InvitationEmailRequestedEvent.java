package com.sqljudge.auth.event;

import java.util.UUID;

public record InvitationEmailRequestedEvent(
        UUID userId, String email, String username, String token) {

    @Override
    public String toString() {
        return "InvitationEmailRequestedEvent[userId=" + userId + "]";
    }
}
