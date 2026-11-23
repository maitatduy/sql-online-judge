package com.sqljudge.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.common.security.Role;
import java.util.UUID;

public record InternalUserAccountResponse(
        UUID userId, String username, Role role, @JsonProperty("isActive") boolean isActive) {}
