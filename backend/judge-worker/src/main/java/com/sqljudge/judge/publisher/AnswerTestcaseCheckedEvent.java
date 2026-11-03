package com.sqljudge.judge.publisher;

import com.sqljudge.judge.job.DialectCheck;
import com.sqljudge.judge.job.ExpectedTable;
import com.sqljudge.judge.job.TestcaseCheckResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnswerTestcaseCheckedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID checkId,
        UUID testcaseId,
        int position,
        int testcaseCount,
        String systemError,
        ExpectedTable expected,
        List<DialectCheck> dialects) {

    static final int VERSION = 1;

    static AnswerTestcaseCheckedEvent of(
            TestcaseCheckResult result, UUID eventId, Instant occurredAt) {
        return new AnswerTestcaseCheckedEvent(
                eventId,
                occurredAt,
                VERSION,
                result.checkId(),
                result.testcaseId(),
                result.position(),
                result.testcaseCount(),
                result.systemError(),
                result.expected(),
                result.dialects());
    }
}
