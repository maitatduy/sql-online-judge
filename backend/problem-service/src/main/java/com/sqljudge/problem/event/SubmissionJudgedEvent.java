package com.sqljudge.problem.event;

import java.time.Instant;
import java.util.UUID;

public record SubmissionJudgedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID submissionId,
        UUID userId,
        UUID problemId,
        String verdict,
        Instant judgedAt) {

    private static final String ACCEPTED = "AC";
    private static final String SYSTEM_ERROR = "SE";

    public boolean isAccepted() {
        return ACCEPTED.equals(verdict);
    }

    public boolean isSystemError() {
        return SYSTEM_ERROR.equals(verdict);
    }
}
