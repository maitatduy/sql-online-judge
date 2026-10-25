package com.sqljudge.auth.repository;

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
        Instant revokedAt) {}
