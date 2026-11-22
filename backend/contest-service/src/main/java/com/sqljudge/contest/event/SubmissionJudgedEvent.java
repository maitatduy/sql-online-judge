package com.sqljudge.contest.event;

import java.time.Instant;
import java.util.UUID;

public record SubmissionJudgedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID submissionId,
        UUID userId,
        UUID problemId,
        UUID contestId,
        String verdict,
        Instant submittedAt,
        Instant judgedAt) {}
