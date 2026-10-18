package com.sqljudge.auth.dto.response;

import com.sqljudge.common.security.Role;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id, String username, String displayName, Role role, int currentStreakDays) {}
