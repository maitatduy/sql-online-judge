package com.sqljudge.judge.job;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariant;
import com.sqljudge.judge.client.SnapshotMapper;
import com.sqljudge.judge.client.SnapshotSource;
import com.sqljudge.judge.client.SnapshotUnavailableException;
import com.sqljudge.judge.compare.CompareOptions;
import com.sqljudge.judge.compare.ComparisonResult;
import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import com.sqljudge.judge.compare.MismatchReason;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.ResultComparator;
import com.sqljudge.judge.compare.ResultNormalizer;
import com.sqljudge.judge.compare.UnsupportedColumnTypeException;
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.guard.GuardDecision;
import com.sqljudge.judge.guard.GuardPolicy;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.sandbox.Dialect;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.RunLimits;
import com.sqljudge.judge.sandbox.Sandbox;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class AnswerCheckProcessor {

    private static final String UNSUPPORTED_COLUMN_TYPE = "Kiểu dữ liệu của cột không được hỗ trợ";

    private final StatementGuard guard;
    private final SnapshotSource snapshots;
    private final SandboxRegistry sandboxes;
    private final ResultNormalizer normalizer = new ResultNormalizer();
    private final ResultComparator comparator = new ResultComparator();

    public AnswerCheckProcessor(
            StatementGuard guard, SnapshotSource snapshots, SandboxRegistry sandboxes) {
        this.guard = guard;
        this.snapshots = snapshots;
        this.sandboxes = sandboxes;
    }

    public void process(AnswerCheckRequest request, Consumer<TestcaseCheckResult> sink) {
        DatasetSnapshotResponse snapshot;
        try {
            snapshot = snapshots.snapshotOf(request.datasetVersionId(), request.datasetChecksum());
        } catch (SnapshotUnavailableException exception) {
            reportSystemError(request, "snapshot_unavailable", sink);
            return;
        }
        Map<Dialect, GuardDecision.Rejected> rejections = rejectionsOf(request);
        RunLimits limits = RunLimits.forTimeLimit(request.timeLimitMs());
        List<AnswerCheckTestcase> testcases = request.testcases();
        for (int index = 0; index < testcases.size(); index++) {
            AnswerCheckTestcase testcase = testcases.get(index);
            Optional<SnapshotVariant> variant = variantOf(snapshot, testcase.variantId());
            if (variant.isEmpty()) {
                sink.accept(systemError(request, testcase, index, "variant_missing"));
                continue;
            }
            QueryContext context = new QueryContext(snapshot, variant.get(), limits, rejections);
            sink.accept(check(request, testcase, index, context));
        }
    }

    private TestcaseCheckResult check(
            AnswerCheckRequest request,
            AnswerCheckTestcase testcase,
            int index,
            QueryContext context) {
        NormalizedTable expected = null;
        List<DialectCheck> dialects = new ArrayList<>();
        for (Map.Entry<Dialect, String> solution : request.solutions().entrySet()) {
            boolean isReference = dialects.isEmpty();
            DialectRun run = runSolution(solution.getKey(), solution.getValue(), context);
            if (isReference && run.table() != null) {
                expected = run.table();
            }
            dialects.add(
                    run.table() == null
                            ? run.check()
                            : compared(run, expected, request.compareOptions()));
        }
        return new TestcaseCheckResult(
                request.checkId(),
                testcase.testcaseId(),
                index + 1,
                request.testcases().size(),
                null,
                expected == null ? null : ExpectedTable.of(expected),
                dialects);
    }

    private DialectCheck compared(
            DialectRun run, NormalizedTable expected, CompareOptions options) {
        DialectCheck check = run.check();
        if (expected == null) {
            return check;
        }
        ComparisonResult comparison = comparator.compare(expected, run.table(), options);
        return new DialectCheck(
                check.dialect(),
                comparison.verdict(),
                comparison.reason(),
                null,
                check.runtimeMs(),
                check.rowCount());
    }

    private DialectRun runSolution(Dialect dialect, String sql, QueryContext context) {
        GuardDecision.Rejected rejected = context.rejections().get(dialect);
        if (rejected != null) {
            return DialectRun.failed(dialect, rejected.verdict(), rejected.message().text(), 0);
        }
        Optional<Sandbox> sandbox = sandboxes.forDialect(dialect);
        if (sandbox.isEmpty()) {
            return DialectRun.failed(dialect, Verdict.SE, null, 0);
        }
        QueryRun run =
                new QueryRun(
                        SnapshotMapper.toDataset(context.snapshot(), context.variant()),
                        sql,
                        context.limits());
        return outcomeOf(dialect, sandbox.get().run(run));
    }

    private DialectRun outcomeOf(Dialect dialect, ExecutionOutcome outcome) {
        long runtimeMs = outcome.runtimeMs();
        if (outcome instanceof Succeeded succeeded) {
            return normalized(dialect, succeeded);
        }
        if (outcome instanceof Failed failed) {
            return DialectRun.failed(dialect, Verdict.RE, failed.message().text(), runtimeMs);
        }
        if (outcome instanceof TimedOut) {
            return DialectRun.failed(dialect, Verdict.TLE, null, runtimeMs);
        }
        return DialectRun.failed(dialect, Verdict.SE, null, runtimeMs);
    }

    private DialectRun normalized(Dialect dialect, Succeeded succeeded) {
        NormalizedTable table;
        try {
            table = normalizer.normalize(succeeded.table());
        } catch (UnsupportedColumnTypeException exception) {
            return DialectRun.failed(
                    dialect, Verdict.RE, UNSUPPORTED_COLUMN_TYPE, succeeded.runtimeMs());
        }
        DialectCheck check =
                new DialectCheck(
                        dialect,
                        null,
                        MismatchReason.NONE,
                        null,
                        succeeded.runtimeMs(),
                        table.rows().size());
        return new DialectRun(check, table);
    }

    private Map<Dialect, GuardDecision.Rejected> rejectionsOf(AnswerCheckRequest request) {
        GuardPolicy policy =
                request.allowedStatements().isEmpty()
                        ? GuardPolicy.selectOnly()
                        : GuardPolicy.of(request.allowedStatements());
        Map<Dialect, GuardDecision.Rejected> rejections = new EnumMap<>(Dialect.class);
        request.solutions()
                .forEach(
                        (dialect, sql) -> {
                            if (guard.check(sql, policy)
                                    instanceof GuardDecision.Rejected rejected) {
                                rejections.put(dialect, rejected);
                            }
                        });
        return rejections;
    }

    private static Optional<SnapshotVariant> variantOf(
            DatasetSnapshotResponse snapshot, UUID variantId) {
        return snapshot.variants().stream()
                .filter(variant -> variant.id().equals(variantId))
                .findFirst();
    }

    private static void reportSystemError(
            AnswerCheckRequest request, String reason, Consumer<TestcaseCheckResult> sink) {
        List<AnswerCheckTestcase> testcases = request.testcases();
        for (int index = 0; index < testcases.size(); index++) {
            sink.accept(systemError(request, testcases.get(index), index, reason));
        }
    }

    private static TestcaseCheckResult systemError(
            AnswerCheckRequest request, AnswerCheckTestcase testcase, int index, String reason) {
        return new TestcaseCheckResult(
                request.checkId(),
                testcase.testcaseId(),
                index + 1,
                request.testcases().size(),
                reason,
                null,
                List.of());
    }

    private record QueryContext(
            DatasetSnapshotResponse snapshot,
            SnapshotVariant variant,
            RunLimits limits,
            Map<Dialect, GuardDecision.Rejected> rejections) {}

    private record DialectRun(DialectCheck check, NormalizedTable table) {

        static DialectRun failed(Dialect dialect, Verdict verdict, String message, long runtimeMs) {
            return new DialectRun(
                    new DialectCheck(dialect, verdict, MismatchReason.NONE, message, runtimeMs, 0),
                    null);
        }
    }
}
