package com.sqljudge.judge.job;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariant;
import com.sqljudge.judge.client.SnapshotMapper;
import com.sqljudge.judge.client.SnapshotSource;
import com.sqljudge.judge.client.SnapshotUnavailableException;
import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.guard.GuardDecision;
import com.sqljudge.judge.guard.GuardPolicy;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.job.StudioRunResult.Rejection;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.RunLimits;
import com.sqljudge.judge.sandbox.Sandbox;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class StudioRunProcessor {

    public static final int PREVIEW_BUDGET_CHARS = 512_000;
    private static final int MAX_TIME_LIMIT_MS = 6_000;
    private static final String TIMED_OUT_MESSAGE = "Câu lệnh chạy quá thời gian cho phép";

    private final StatementGuard guard;
    private final SnapshotSource snapshots;
    private final SandboxRegistry sandboxes;

    public StudioRunProcessor(
            StatementGuard guard, SnapshotSource snapshots, SandboxRegistry sandboxes) {
        this.guard = guard;
        this.snapshots = snapshots;
        this.sandboxes = sandboxes;
    }

    public StudioRunResult process(StudioRunRequest request) {
        GuardDecision decision = guard.check(request.sql(), GuardPolicy.selectOnly());
        if (decision instanceof GuardDecision.Rejected rejected) {
            return StudioRunResult.rejected(
                    request, new Rejection(rejected.verdict(), rejected.message().text()));
        }
        DatasetSnapshotResponse snapshot;
        try {
            snapshot = snapshots.snapshotOf(request.datasetVersionId(), request.datasetChecksum());
        } catch (SnapshotUnavailableException exception) {
            return StudioRunResult.systemError(request, "snapshot_unavailable");
        }
        Optional<Sandbox> sandbox = sandboxes.forDialect(request.dialect());
        if (sandbox.isEmpty()) {
            return StudioRunResult.systemError(request, "dialect_unavailable");
        }
        RunLimits limits = limitsOf(request.timeLimitMs());
        ResultPreview preview = new ResultPreview(PREVIEW_BUDGET_CHARS);
        List<VariantRunResult> results = new ArrayList<>();
        for (SnapshotVariant variant : snapshot.variants()) {
            ExecutionOutcome outcome =
                    sandbox.get()
                            .run(
                                    new QueryRun(
                                            SnapshotMapper.toDataset(snapshot, variant),
                                            request.sql(),
                                            limits));
            results.add(resultOf(variant, outcome, preview));
        }
        return StudioRunResult.completed(request, results);
    }

    private static VariantRunResult resultOf(
            SnapshotVariant variant, ExecutionOutcome outcome, ResultPreview preview) {
        if (outcome instanceof Succeeded succeeded) {
            ResultTable table = succeeded.table();
            List<List<Object>> rows = preview.take(table.rows());
            return new VariantRunResult(
                    variant.id(),
                    variant.name(),
                    variant.isPublic(),
                    RunStatus.SUCCEEDED,
                    table.columnNames(),
                    rows,
                    table.rows().size(),
                    rows.size() < table.rows().size(),
                    succeeded.runtimeMs(),
                    null);
        }
        return failureOf(variant, outcome);
    }

    private static VariantRunResult failureOf(SnapshotVariant variant, ExecutionOutcome outcome) {
        RunStatus status = RunStatus.SANDBOX_FAILED;
        String message = null;
        if (outcome instanceof Failed failed) {
            status = RunStatus.FAILED;
            message = failed.message().text();
        } else if (outcome instanceof TimedOut) {
            status = RunStatus.TIMED_OUT;
            message = TIMED_OUT_MESSAGE;
        }
        return new VariantRunResult(
                variant.id(),
                variant.name(),
                variant.isPublic(),
                status,
                List.of(),
                List.of(),
                0,
                false,
                outcome.runtimeMs(),
                message);
    }

    private static RunLimits limitsOf(int timeLimitMs) {
        RunLimits defaults = RunLimits.defaults();
        long timeoutMs =
                timeLimitMs > 0
                        ? Math.min(timeLimitMs, MAX_TIME_LIMIT_MS)
                        : defaults.timeout().toMillis();
        return new RunLimits(
                Duration.ofMillis(timeoutMs), defaults.maxRows(), defaults.maxResultBytes());
    }
}
