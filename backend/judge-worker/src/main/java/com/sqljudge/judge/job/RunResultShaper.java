package com.sqljudge.judge.job;

import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.ResultNormalizer;
import com.sqljudge.judge.compare.Row;
import com.sqljudge.judge.compare.UnsupportedColumnTypeException;
import com.sqljudge.judge.job.ExpectedTable.WireCell;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class RunResultShaper {

    static final int MAX_ROWS = 100;
    private static final int MAX_CELL_CHARS = 500;
    private static final int BUDGET_CHARS = 200_000;
    private static final int NULL_CHARS = 4;
    private static final String ELLIPSIS = "…";
    private static final String UNSUPPORTED_COLUMN_TYPE = "Kiểu dữ liệu của cột không được hỗ trợ";

    private final ResultNormalizer normalizer = new ResultNormalizer();

    CodeRunResult resultOf(UUID runId, ExecutionOutcome outcome) {
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
                    ExpectedTable.wireRow(row).stream().map(RunResultShaper::textOf).toList();
            int rowChars = cells.stream().mapToInt(RunResultShaper::charsOf).sum();
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
}
