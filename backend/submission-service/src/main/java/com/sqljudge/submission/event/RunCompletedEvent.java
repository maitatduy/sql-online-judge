package com.sqljudge.submission.event;

import com.sqljudge.submission.entity.CodeRun.RunCompletion;
import com.sqljudge.submission.entity.RunOutcome;
import com.sqljudge.submission.entity.RunResult;
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

    public RunCompletion toCompletion() {
        return new RunCompletion(
                outcome, new RunResult(columns, rows, isTruncated), runtimeMs, message);
    }
}
