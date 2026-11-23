package com.sqljudge.problem.event;

import java.time.Instant;
import java.util.UUID;

public record RejudgeFinishedEvent(
        UUID eventId, Instant occurredAt, int version, UUID problemId, UUID batchId) {}
