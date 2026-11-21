package com.sqljudge.judge.job;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariant;
import com.sqljudge.judge.client.SnapshotMapper;
import com.sqljudge.judge.client.SnapshotSource;
import com.sqljudge.judge.client.SnapshotUnavailableException;
import com.sqljudge.judge.guard.GuardDecision;
import com.sqljudge.judge.guard.GuardPolicy;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.RunLimits;
import com.sqljudge.judge.sandbox.Sandbox;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LessonRunProcessor {

    private static final Logger log = LoggerFactory.getLogger(LessonRunProcessor.class);

    private final StatementGuard guard;
    private final SnapshotSource snapshots;
    private final SandboxRegistry sandboxes;
    private final RunResultShaper shaper = new RunResultShaper();

    public LessonRunProcessor(
            StatementGuard guard, SnapshotSource snapshots, SandboxRegistry sandboxes) {
        this.guard = guard;
        this.snapshots = snapshots;
        this.sandboxes = sandboxes;
    }

    public CodeRunResult run(LessonRunRequest request) {
        if (guard.check(request.sql(), GuardPolicy.selectOnly())
                instanceof GuardDecision.Rejected rejected) {
            return CodeRunResult.failed(
                    request.runId(), RunOutcome.FS, 0, rejected.message().text());
        }
        Optional<Sandbox> sandbox = sandboxes.forDialect(request.dialect());
        Optional<DatasetSnapshotResponse> snapshot = snapshotOf(request);
        Optional<SnapshotVariant> variant =
                snapshot.flatMap(loaded -> publicVariantOf(loaded, request));
        if (sandbox.isEmpty() || variant.isEmpty()) {
            return CodeRunResult.failed(request.runId(), RunOutcome.SE, 0, null);
        }
        QueryRun run =
                new QueryRun(
                        SnapshotMapper.toDataset(snapshot.get(), variant.get()),
                        request.sql(),
                        RunLimits.forTimeLimit(request.timeLimitMs()));
        return shaper.resultOf(request.runId(), sandbox.get().run(run));
    }

    private Optional<DatasetSnapshotResponse> snapshotOf(LessonRunRequest request) {
        try {
            return Optional.of(
                    snapshots.snapshotOf(request.datasetVersionId(), request.datasetChecksum()));
        } catch (SnapshotUnavailableException exception) {
            log.warn(
                    "lesson_run_snapshot_unavailable runId={} versionId={}",
                    request.runId(),
                    request.datasetVersionId(),
                    exception);
            return Optional.empty();
        }
    }

    private static Optional<SnapshotVariant> publicVariantOf(
            DatasetSnapshotResponse snapshot, LessonRunRequest request) {
        Optional<SnapshotVariant> variant =
                snapshot.variants().stream()
                        .filter(candidate -> candidate.id().equals(request.datasetVariantId()))
                        .filter(SnapshotVariant::isPublic)
                        .findFirst();
        if (variant.isEmpty()) {
            log.warn(
                    "lesson_run_variant_refused runId={} variantId={}",
                    request.runId(),
                    request.datasetVariantId());
        }
        return variant;
    }
}
