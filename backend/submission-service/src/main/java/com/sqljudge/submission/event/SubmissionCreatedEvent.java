package com.sqljudge.submission.event;

import com.sqljudge.common.outbox.DomainEvent;
import com.sqljudge.submission.entity.Dialect;
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
        int attempt)
        implements DomainEvent {}
