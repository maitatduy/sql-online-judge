package com.sqljudge.problem.event;

import com.sqljudge.common.outbox.DomainEvent;
import com.sqljudge.problem.entity.Dialect;
import java.time.Instant;
import java.util.UUID;

public record StudioRunRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID runId,
        UUID datasetVersionId,
        String datasetChecksum,
        Dialect dialect,
        String sql,
        int timeLimitMs)
        implements DomainEvent {}
