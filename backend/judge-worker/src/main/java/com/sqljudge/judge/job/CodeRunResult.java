package com.sqljudge.judge.job;

import java.util.List;
import java.util.UUID;

public record CodeRunResult(
        UUID runId,
        RunOutcome outcome,
        List<String> columns,
        List<List<String>> rows,
        boolean isTruncated,
        long runtimeMs,
        String message) {

    public CodeRunResult {
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
    }

    public static CodeRunResult failed(
            UUID runId, RunOutcome outcome, long runtimeMs, String message) {
        return new CodeRunResult(runId, outcome, List.of(), List.of(), false, runtimeMs, message);
    }
}
