package com.sqljudge.judge.job;

import com.sqljudge.judge.compare.MismatchReason;
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.sandbox.Dialect;

public record DialectCheck(
        Dialect dialect,
        Verdict verdict,
        MismatchReason mismatch,
        String message,
        long runtimeMs,
        int rowCount) {}
