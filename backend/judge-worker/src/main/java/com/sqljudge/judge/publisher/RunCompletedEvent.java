package com.sqljudge.judge.publisher;

import com.sqljudge.judge.job.CodeRunResult;
import com.sqljudge.judge.job.RunOutcome;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RunCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID runId,
        RunOutcome outcome,
        List<String> columns,
        List<List<String>> rows,
        boolean isTruncated,
        long runtimeMs,
        String message) {

    private static final int EVENT_VERSION = 1;

    static RunCompletedEvent of(CodeRunResult result, UUID eventId, Instant occurredAt) {
        return new RunCompletedEvent(
                eventId,
                occurredAt,
                EVENT_VERSION,
                result.runId(),
                result.outcome(),
                result.columns(),
                result.rows(),
                result.isTruncated(),
                result.runtimeMs(),
                result.message());
    }
}
