package com.sqljudge.judge.job;

import com.sqljudge.judge.compare.Verdict;
import java.util.UUID;

public record SubmissionJudgement(
        UUID submissionId,
        int attempt,
        Verdict verdict,
        long runtimeMs,
        Integer failedIndex,
        Boolean failedIsHidden,
        String message,
        ResultDiff diff,
        int judgedTestcases,
        int totalTestcases) {}
