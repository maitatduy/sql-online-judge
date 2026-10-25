package com.sqljudge.auth.event;

import com.sqljudge.common.security.Role;
import java.util.UUID;

public record RoleInvitationEmailRequestedEvent(
        UUID invitationId,
        String email,
        String inviterUsername,
        Role role,
        String message,
        String token) {

    @Override
    public String toString() {
        return "RoleInvitationEmailRequestedEvent[invitationId=" + invitationId + "]";
    }
}
