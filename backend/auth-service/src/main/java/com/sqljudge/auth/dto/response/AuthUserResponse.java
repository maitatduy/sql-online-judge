package com.sqljudge.auth.dto.response;

import com.sqljudge.common.security.Role;
import java.util.UUID;

public record AuthUserResponse(UUID id, String username, String email, Role role) {}
