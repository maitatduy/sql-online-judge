package com.sqljudge.community.event;

import com.sqljudge.common.outbox.DomainEvent;
import com.sqljudge.community.entity.ReportReason;
import com.sqljudge.community.entity.ReportTarget;
import java.time.Instant;
import java.util.UUID;

public record ReportedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        ReportTarget targetType,
        UUID targetId,
        ReportReason reason,
        long openReportCount)
        implements DomainEvent {}
