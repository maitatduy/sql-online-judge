package com.sqljudge.judge.metrics;

import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.SandboxFailed;
import com.sqljudge.judge.sandbox.Dialect;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.Sandbox;

public final class MeteredSandbox implements Sandbox {

    private final Dialect dialect;
    private final Sandbox delegate;
    private final JudgeMetrics metrics;

    public MeteredSandbox(Dialect dialect, Sandbox delegate, JudgeMetrics metrics) {
        this.dialect = dialect;
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public ExecutionOutcome run(QueryRun run) {
        ExecutionOutcome outcome = delegate.run(run);
        if (outcome instanceof SandboxFailed failed) {
            metrics.sandboxFailed(dialect, failed.reason());
        }
        return outcome;
    }
}
