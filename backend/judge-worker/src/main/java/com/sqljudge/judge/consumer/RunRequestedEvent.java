package com.sqljudge.judge.consumer;

import com.sqljudge.judge.sandbox.Dialect;
import java.time.Instant;
import java.util.UUID;

public record RunRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID runId,
        UUID revisionId,
        Dialect dialect,
        String code) {}
