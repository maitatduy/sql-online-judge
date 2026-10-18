package com.sqljudge.auth.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserSessionResponse(
        UUID id,
        String browser,
        String os,
        String city,
        Instant lastActiveAt,
        boolean isActiveNow,
        boolean isUnusualLocation) {}
