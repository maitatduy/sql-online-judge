package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.MismatchReason;
import com.sqljudge.problem.entity.Verdict;

public record DialectCheckResult(
        Dialect dialect,
        Verdict verdict,
        MismatchReason mismatch,
        String message,
        long runtimeMs,
        int rowCount) {}
