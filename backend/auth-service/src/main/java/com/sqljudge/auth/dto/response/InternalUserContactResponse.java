package com.sqljudge.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record InternalUserContactResponse(
        UUID userId,
        String username,
        String email,
        @JsonProperty("isEmailVerified") boolean isEmailVerified) {}
