package com.sqljudge.judge.publisher;

import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.job.ResultDiff;
import com.sqljudge.judge.job.SubmissionJudgement;
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
        ResultDiff diff,
        int judgedTestcases,
        int totalTestcases) {

    private static final int EVENT_VERSION = 1;

    static JudgeCompletedEvent of(SubmissionJudgement judgement, UUID eventId, Instant occurredAt) {
        return new JudgeCompletedEvent(
                eventId,
                occurredAt,
                EVENT_VERSION,
                judgement.submissionId(),
                judgement.attempt(),
                judgement.verdict(),
                judgement.runtimeMs(),
                judgement.failedIndex(),
                judgement.failedIsHidden(),
                judgement.message(),
                judgement.diff(),
                judgement.judgedTestcases(),
                judgement.totalTestcases());
    }
}
