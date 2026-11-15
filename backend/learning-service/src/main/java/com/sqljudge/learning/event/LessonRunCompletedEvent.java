package com.sqljudge.learning.event;

import com.sqljudge.learning.entity.LessonRun.RunCompletion;
import com.sqljudge.learning.entity.RunOutcome;
import com.sqljudge.learning.entity.RunResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LessonRunCompletedEvent(
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
