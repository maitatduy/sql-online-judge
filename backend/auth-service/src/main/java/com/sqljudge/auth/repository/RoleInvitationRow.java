package com.sqljudge.auth.repository;

import com.sqljudge.auth.entity.InvitationTimeline;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.UUID;

public record RoleInvitationRow(
        UUID id,
        String email,
        Role role,
        String inviterUsername,
        Instant invitedAt,
        Instant expiresAt,
        Instant acceptedAt,
        Instant revokedAt,
        Instant declinedAt) {

    public InvitationTimeline timeline() {
        return new InvitationTimeline(acceptedAt, revokedAt, declinedAt, expiresAt);
    }
}
