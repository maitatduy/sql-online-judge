package com.sqljudge.submission.event;

import com.sqljudge.common.outbox.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record RejudgeFinishedEvent(
        UUID eventId, Instant occurredAt, int version, UUID problemId, UUID batchId)
        implements DomainEvent {}
