package com.sqljudge.contest.event;

import com.sqljudge.common.outbox.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContestRatingUpdatedEvent(
        UUID eventId, Instant occurredAt, int version, UUID contestId, List<RatingChange> changes)
        implements DomainEvent {

    public ContestRatingUpdatedEvent {
        changes = List.copyOf(changes);
    }

    public record RatingChange(UUID userId, int newRating, int delta) {}
}
