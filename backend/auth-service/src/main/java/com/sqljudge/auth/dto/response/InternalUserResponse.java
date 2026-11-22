package com.sqljudge.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record InternalUserResponse(
        UUID userId,
        String username,
        String displayName,
        @JsonProperty("isBannedFromRatedContests") boolean isBannedFromRatedContests) {}
