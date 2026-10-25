package com.sqljudge.auth.dto.response;

import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.UUID;

public record AuditLogEntryResponse(
        UUID id,
        Instant occurredAt,
        String actorUsername,
        Role actorRole,
        String targetUsername,
        AdminActionType action,
        String reason,
        String previousValue,
        String newValue,
        Instant expiresAt,
        String details) {}
