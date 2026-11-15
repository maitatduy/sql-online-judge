package com.sqljudge.judge.job;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariant;
import com.sqljudge.judge.client.SnapshotMapper;
import com.sqljudge.judge.client.SnapshotSource;
import com.sqljudge.judge.client.SnapshotUnavailableException;
import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.guard.GuardDecision;
import com.sqljudge.judge.guard.GuardPolicy;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.job.JudgePackage.JudgeCase;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.RunLimits;
import com.sqljudge.judge.sandbox.Sandbox;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CodeRunProcessor {

    public static final int MAX_ROWS = RunResultShaper.MAX_ROWS;

    private final StatementGuard guard;
    private final SnapshotSource snapshots;
    private final SandboxRegistry sandboxes;
    private final RunResultShaper shaper = new RunResultShaper();

    public CodeRunProcessor(
            StatementGuard guard, SnapshotSource snapshots, SandboxRegistry sandboxes) {
        this.guard = guard;
        this.snapshots = snapshots;
        this.sandboxes = sandboxes;
    }

    public CodeRunResult run(CodeRunRequest request) {
        UUID runId = request.runId();
        JudgePackage judgePackage = request.judgePackage();
        if (guard.check(request.code(), policyOf(judgePackage))
                instanceof GuardDecision.Rejected rejected) {
            return CodeRunResult.failed(runId, RunOutcome.FS, 0, rejected.message().text());
        }
        Optional<Sandbox> sandbox = sandboxes.forDialect(request.dialect());
        Optional<DatasetSnapshotResponse> loaded = snapshotOf(judgePackage);
        if (sandbox.isEmpty() || loaded.isEmpty()) {
            return CodeRunResult.failed(runId, RunOutcome.SE, 0, null);
        }
        DatasetSnapshotResponse snapshot = loaded.get();
        Optional<SnapshotVariant> sample = sampleOf(judgePackage, snapshot);
        if (sample.isEmpty()) {
            return CodeRunResult.failed(runId, RunOutcome.SE, 0, null);
        }
        ExecutionOutcome outcome =
                sandbox.get()
                        .run(
                                new QueryRun(
                                        SnapshotMapper.toDataset(snapshot, sample.get()),
                                        request.code(),
                                        RunLimits.forTimeLimit(judgePackage.timeLimitMs())));
        return shaper.resultOf(runId, outcome);
    }

    private static Optional<SnapshotVariant> sampleOf(
            JudgePackage judgePackage, DatasetSnapshotResponse snapshot) {
        return judgePackage.testcases().stream()
                .filter(JudgeCase::isPublic)
                .min(Comparator.comparingInt(JudgeCase::index))
                .map(JudgeCase::variantId)
                .flatMap(
                        id ->
                                snapshot.variants().stream()
                                        .filter(variant -> variant.id().equals(id))
                                        .findFirst());
    }

    private Optional<DatasetSnapshotResponse> snapshotOf(JudgePackage judgePackage) {
        try {
            return Optional.of(
                    snapshots.snapshotOf(
                            judgePackage.datasetVersionId(), judgePackage.datasetChecksum()));
        } catch (SnapshotUnavailableException exception) {
            return Optional.empty();
        }
    }

    private static GuardPolicy policyOf(JudgePackage judgePackage) {
        return judgePackage.allowedStatements().isEmpty()
                ? GuardPolicy.selectOnly()
                : GuardPolicy.of(judgePackage.allowedStatements());
    }
}
