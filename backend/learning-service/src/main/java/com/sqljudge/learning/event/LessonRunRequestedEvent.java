package com.sqljudge.learning.event;

import com.sqljudge.common.outbox.DomainEvent;
import com.sqljudge.learning.entity.Dialect;
import java.time.Instant;
import java.util.UUID;

public record LessonRunRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID runId,
        UUID datasetVersionId,
        UUID datasetVariantId,
        String datasetChecksum,
        Dialect dialect,
        String sql,
        int timeLimitMs)
        implements DomainEvent {}
