package com.sqljudge.judge.compare;

public sealed interface ExecutionOutcome {

    long runtimeMs();

    record Succeeded(ResultTable table, long runtimeMs) implements ExecutionOutcome {}

    record Failed(SanitizedMessage message, long runtimeMs) implements ExecutionOutcome {}

    record TimedOut(long runtimeMs) implements ExecutionOutcome {}

    record SandboxFailed(String reason, long runtimeMs) implements ExecutionOutcome {}
}
