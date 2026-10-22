package com.sqljudge.judge.compare;

import java.util.List;

public record JudgeResult(
        Verdict verdict,
        Integer firstFailedCase,
        long maxRuntimeMs,
        List<TestcaseOutcome> testcases,
        ComparisonResult failedComparison,
        String errorMessage) {

    public JudgeResult {
        testcases = List.copyOf(testcases);
    }
}
