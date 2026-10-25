package com.sqljudge.auth.dto.response;

import com.sqljudge.auth.entity.InvitationStatus;
import com.sqljudge.common.security.Role;
import java.time.Instant;

public record RoleInvitationViewResponse(
        String maskedEmail,
        Role role,
        String inviterUsername,
        Instant expiresAt,
        InvitationStatus status) {}
