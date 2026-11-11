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
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.ResultNormalizer;
import com.sqljudge.judge.compare.Row;
import com.sqljudge.judge.compare.UnsupportedColumnTypeException;
import com.sqljudge.judge.guard.GuardDecision;
import com.sqljudge.judge.guard.GuardPolicy;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.job.ExpectedTable.WireCell;
import com.sqljudge.judge.job.JudgePackage.JudgeCase;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.RunLimits;
import com.sqljudge.judge.sandbox.Sandbox;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CodeRunProcessor {

    public static final int MAX_ROWS = 100;
    private static final int MAX_CELL_CHARS = 500;
    private static final int BUDGET_CHARS = 200_000;
    private static final int NULL_CHARS = 4;
    private static final String ELLIPSIS = "…";
    private static final String UNSUPPORTED_COLUMN_TYPE = "Kiểu dữ liệu của cột không được hỗ trợ";

    private final StatementGuard guard;
    private final SnapshotSource snapshots;
    private final SandboxRegistry sandboxes;
    private final ResultNormalizer normalizer = new ResultNormalizer();

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
        return resultOf(runId, outcome);
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

    private CodeRunResult resultOf(UUID runId, ExecutionOutcome outcome) {
        long runtimeMs = outcome.runtimeMs();
        if (outcome instanceof Failed failed) {
            return CodeRunResult.failed(runId, RunOutcome.RE, runtimeMs, failed.message().text());
        }
        if (outcome instanceof TimedOut) {
            return CodeRunResult.failed(runId, RunOutcome.TLE, runtimeMs, null);
        }
        if (!(outcome instanceof Succeeded succeeded)) {
            return CodeRunResult.failed(runId, RunOutcome.SE, runtimeMs, null);
        }
        NormalizedTable table;
        try {
            table = normalizer.normalize(succeeded.table());
        } catch (UnsupportedColumnTypeException exception) {
            return CodeRunResult.failed(runId, RunOutcome.RE, runtimeMs, UNSUPPORTED_COLUMN_TYPE);
        }
        List<List<String>> rows = previewOf(table.rows());
        return new CodeRunResult(
                runId,
                RunOutcome.OK,
                table.columnNames(),
                rows,
                rows.size() < table.rows().size(),
                runtimeMs,
                null);
    }

    private static List<List<String>> previewOf(List<Row> rows) {
        List<List<String>> preview = new ArrayList<>();
        int remainingChars = BUDGET_CHARS;
        for (Row row : rows) {
            if (preview.size() == MAX_ROWS) {
                break;
            }
            List<String> cells =
                    ExpectedTable.wireRow(row).stream().map(CodeRunProcessor::textOf).toList();
            int rowChars = cells.stream().mapToInt(CodeRunProcessor::charsOf).sum();
            if (rowChars > remainingChars) {
                break;
            }
            remainingChars -= rowChars;
            preview.add(cells);
        }
        return preview;
    }

    private static String textOf(WireCell cell) {
        String value = cell.value();
        if (value == null || value.length() <= MAX_CELL_CHARS) {
            return value;
        }
        return value.substring(0, MAX_CELL_CHARS) + ELLIPSIS;
    }

    private static int charsOf(String cell) {
        return cell == null ? NULL_CHARS : cell.length();
    }

    private static GuardPolicy policyOf(JudgePackage judgePackage) {
        return judgePackage.allowedStatements().isEmpty()
                ? GuardPolicy.selectOnly()
                : GuardPolicy.of(judgePackage.allowedStatements());
    }
}
