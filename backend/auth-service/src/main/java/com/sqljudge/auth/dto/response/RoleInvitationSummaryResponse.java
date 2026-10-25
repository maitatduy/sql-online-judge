package com.sqljudge.auth.dto.response;

import com.sqljudge.auth.entity.InvitationStatus;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.UUID;

public record RoleInvitationSummaryResponse(
        UUID id,
        String email,
        Role role,
        InvitationStatus status,
        String inviterUsername,
        Instant invitedAt,
        Instant expiresAt,
        Instant acceptedAt,
        Instant revokedAt) {}
