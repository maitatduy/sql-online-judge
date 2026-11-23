package com.sqljudge.community.event;

import com.sqljudge.common.outbox.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record CommentedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID commentId,
        UUID postId,
        UUID problemId,
        UUID authorId,
        UUID recipientId)
        implements DomainEvent {}
