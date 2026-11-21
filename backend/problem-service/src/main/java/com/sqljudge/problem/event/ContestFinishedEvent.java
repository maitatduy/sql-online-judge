package com.sqljudge.problem.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContestFinishedEvent(
        UUID eventId, Instant occurredAt, int version, UUID contestId, List<UUID> problemIds) {

    public ContestFinishedEvent {
        problemIds = problemIds == null ? List.of() : List.copyOf(problemIds);
    }
}
