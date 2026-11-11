package com.sqljudge.judge.job;

import com.sqljudge.judge.compare.ComparisonResult;
import com.sqljudge.judge.compare.MismatchReason;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.Row;
import com.sqljudge.judge.job.ExpectedTable.WireCell;
import java.util.List;

public record ResultDiff(
        MismatchReason reason,
        List<String> expectedColumns,
        List<String> actualColumns,
        int expectedRowCount,
        int actualRowCount,
        List<List<WireCell>> missingRows,
        List<List<WireCell>> extraRows,
        OrderMismatch orderMismatch) {

    public static final int MAX_ROWS = 20;
    public static final int MAX_COLUMNS = 20;
    public static final int MAX_CELL_CHARS = 200;
    private static final String ELLIPSIS = "…";

    public ResultDiff {
        expectedColumns = List.copyOf(expectedColumns);
        actualColumns = List.copyOf(actualColumns);
        missingRows = List.copyOf(missingRows);
        extraRows = List.copyOf(extraRows);
    }

    public record OrderMismatch(int rowNumber, List<WireCell> expected, List<WireCell> actual) {}

    public static ResultDiff of(
            NormalizedTable expected, NormalizedTable actual, ComparisonResult comparison) {
        return new ResultDiff(
                comparison.reason(),
                shortened(expected.columnNames()),
                shortened(actual.columnNames()),
                expected.rows().size(),
                actual.rows().size(),
                rowsAt(expected, comparison.missingRows()),
                rowsAt(actual, comparison.extraRows()),
                orderMismatchOf(expected, actual, comparison.firstOrderMismatchRow()));
    }

    private static OrderMismatch orderMismatchOf(
            NormalizedTable expected, NormalizedTable actual, Integer rowIndex) {
        if (rowIndex == null) {
            return null;
        }
        return new OrderMismatch(
                rowIndex + 1,
                wireRow(expected.rows().get(rowIndex)),
                wireRow(actual.rows().get(rowIndex)));
    }

    private static List<List<WireCell>> rowsAt(NormalizedTable table, List<Integer> indices) {
        return indices.stream()
                .limit(MAX_ROWS)
                .map(index -> wireRow(table.rows().get(index)))
                .toList();
    }

    private static List<WireCell> wireRow(Row row) {
        return ExpectedTable.wireRow(row).stream()
                .limit(MAX_COLUMNS)
                .map(cell -> new WireCell(cell.kind(), shortened(cell.value())))
                .toList();
    }

    private static List<String> shortened(List<String> values) {
        return values.stream().limit(MAX_COLUMNS).map(ResultDiff::shortened).toList();
    }

    private static String shortened(String value) {
        if (value == null || value.length() <= MAX_CELL_CHARS) {
            return value;
        }
        return value.substring(0, MAX_CELL_CHARS) + ELLIPSIS;
    }
}
