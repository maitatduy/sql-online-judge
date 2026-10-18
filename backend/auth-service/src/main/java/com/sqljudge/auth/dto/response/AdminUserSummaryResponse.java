package com.sqljudge.auth.dto.response;

import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.UUID;

public record AdminUserSummaryResponse(
        UUID id,
        String username,
        String email,
        Role role,
        int rating,
        int acceptedCount,
        Instant joinedAt,
        Instant lastActiveAt,
        boolean isOnline,
        AccountStatus status,
        Instant bannedUntil,
        boolean isEmailVerified,
        String avatarUrl,
        Instant deletedAt) {}
