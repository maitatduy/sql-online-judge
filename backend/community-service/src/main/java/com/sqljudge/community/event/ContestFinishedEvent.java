package com.sqljudge.community.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContestFinishedEvent(
        UUID eventId, Instant occurredAt, int version, UUID contestId, List<UUID> problemIds) {

    public ContestFinishedEvent {
        problemIds = List.copyOf(problemIds);
    }
}
