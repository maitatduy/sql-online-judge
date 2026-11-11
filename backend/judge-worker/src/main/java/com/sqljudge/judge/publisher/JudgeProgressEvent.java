package com.sqljudge.judge.publisher;

import com.sqljudge.judge.job.JudgeProgress;
import java.time.Instant;
import java.util.UUID;

public record JudgeProgressEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID submissionId,
        int attempt,
        int judgedTestcases,
        int totalTestcases) {

    private static final int EVENT_VERSION = 1;

    static JudgeProgressEvent of(JudgeProgress progress, UUID eventId, Instant occurredAt) {
        return new JudgeProgressEvent(
                eventId,
                occurredAt,
                EVENT_VERSION,
                progress.submissionId(),
                progress.attempt(),
                progress.judgedTestcases(),
                progress.totalTestcases());
    }
}
