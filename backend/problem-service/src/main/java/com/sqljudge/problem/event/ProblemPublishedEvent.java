package com.sqljudge.problem.event;

import com.sqljudge.problem.entity.Difficulty;
import java.time.Instant;
import java.util.UUID;

public record ProblemPublishedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID problemId,
        int number,
        String slug,
        String title,
        Difficulty difficulty,
        UUID authorId)
        implements DomainEvent {}
