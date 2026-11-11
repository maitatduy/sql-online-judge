package com.sqljudge.submission.event;

import java.time.Instant;
import java.util.UUID;

public record JudgeProgressEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID submissionId,
        int attempt,
        int judgedTestcases,
        int totalTestcases) {}
