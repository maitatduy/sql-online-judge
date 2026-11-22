package com.sqljudge.contest.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.common.outbox.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ContestStartedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID contestId,
        Instant startAt,
        Instant endAt,
        @JsonProperty("isRated") boolean isRated)
        implements DomainEvent {}
