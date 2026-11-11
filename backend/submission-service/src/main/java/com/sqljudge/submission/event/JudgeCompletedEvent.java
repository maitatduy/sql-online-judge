package com.sqljudge.submission.event;

import com.sqljudge.submission.entity.JudgeResult;
import com.sqljudge.submission.entity.Verdict;
import java.time.Instant;
import java.util.UUID;

public record JudgeCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID submissionId,
        int attempt,
        Verdict verdict,
        long runtimeMs,
        Integer failedIndex,
        Boolean failedIsHidden,
        String message,
        int judgedTestcases,
        int totalTestcases) {

    public JudgeResult toResult() {
        return new JudgeResult(
                attempt, verdict, runtimeMs, failedIndex, failedIsHidden, message, judgedTestcases);
    }
}
