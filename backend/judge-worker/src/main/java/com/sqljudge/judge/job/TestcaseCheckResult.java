package com.sqljudge.judge.job;

import java.util.List;
import java.util.UUID;

public record TestcaseCheckResult(
        UUID checkId,
        UUID testcaseId,
        int position,
        int testcaseCount,
        String systemError,
        ExpectedTable expected,
        List<DialectCheck> dialects) {

    public TestcaseCheckResult {
        dialects = List.copyOf(dialects);
    }
}
