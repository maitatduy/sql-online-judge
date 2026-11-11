package com.sqljudge.submission.event;

import com.sqljudge.common.outbox.DomainEvent;
import com.sqljudge.submission.entity.Verdict;
import java.time.Instant;
import java.util.UUID;

public record SubmissionJudgedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID submissionId,
        UUID userId,
        UUID problemId,
        Verdict verdict,
        Instant judgedAt)
        implements DomainEvent {}
