package com.sqljudge.judge.consumer;

import com.sqljudge.judge.sandbox.Dialect;
import java.time.Instant;
import java.util.UUID;

public record SubmissionCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID submissionId,
        UUID revisionId,
        Dialect dialect,
        String code,
        int attempt) {}
