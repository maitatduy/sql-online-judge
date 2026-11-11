package com.sqljudge.submission.event;

import com.sqljudge.common.outbox.DomainEvent;
import com.sqljudge.submission.entity.Dialect;
import java.time.Instant;
import java.util.UUID;

public record RunRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID runId,
        UUID revisionId,
        Dialect dialect,
        String code)
        implements DomainEvent {}
