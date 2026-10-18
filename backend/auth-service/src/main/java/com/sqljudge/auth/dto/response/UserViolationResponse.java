package com.sqljudge.auth.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserViolationResponse(
        UUID id,
        Severity severity,
        String title,
        String detail,
        Status status,
        String handledBy,
        Instant handledAt) {

    public enum Severity {
        SEVERE,
        MINOR
    }

    public enum Status {
        OPEN,
        RESOLVED
    }
}
