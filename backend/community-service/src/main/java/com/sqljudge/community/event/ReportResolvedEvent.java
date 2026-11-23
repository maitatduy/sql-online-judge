package com.sqljudge.community.event;

import com.sqljudge.common.outbox.DomainEvent;
import com.sqljudge.community.entity.ReportAction;
import com.sqljudge.community.entity.ReportTarget;
import java.time.Instant;
import java.util.UUID;

public record ReportResolvedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        ReportTarget targetType,
        UUID targetId,
        UUID authorId,
        ReportAction action)
        implements DomainEvent {}
