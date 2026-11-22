package com.sqljudge.contest.event;

import com.sqljudge.common.outbox.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContestFinishedEvent(
        UUID eventId, Instant occurredAt, int version, UUID contestId, List<UUID> problemIds)
        implements DomainEvent {

    public ContestFinishedEvent {
        problemIds = List.copyOf(problemIds);
    }
}
