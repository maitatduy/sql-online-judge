package com.sqljudge.judge.compare;

import com.sqljudge.judge.compare.Cell.NumberCell;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ResultComparator {

    public ComparisonResult compare(
            NormalizedTable expected, NormalizedTable actual, CompareOptions options) {
        if (expected.columnCount() != actual.columnCount()) {
            return ComparisonResult.rejected(Verdict.WA, MismatchReason.COLUMN_COUNT);
        }
        if (options.compareColumnNames() && !expected.columnNames().equals(actual.columnNames())) {
            return ComparisonResult.rejected(Verdict.WA, MismatchReason.COLUMN_NAMES);
        }
        BigDecimal tolerance = options.tolerance();
        if (options.ordered()) {
            return compareInOrder(expected.rows(), actual.rows(), tolerance);
        }
        return compareAsMultiset(expected.rows(), actual.rows(), tolerance);
    }

    private ComparisonResult compareInOrder(
            List<Row> expected, List<Row> actual, BigDecimal tolerance) {
        if (matchInSequence(expected, actual, tolerance)) {
            return ComparisonResult.accepted();
        }
        ComparisonResult asMultiset = compareAsMultiset(expected, actual, tolerance);
        if (asMultiset.isAccepted()) {
            return ComparisonResult.rejected(Verdict.OM, MismatchReason.ORDER);
        }
        return asMultiset;
    }

    private boolean matchInSequence(List<Row> expected, List<Row> actual, BigDecimal tolerance) {
        if (expected.size() != actual.size()) {
            return false;
        }
        for (int i = 0; i < expected.size(); i++) {
            if (!rowsMatch(expected.get(i), actual.get(i), tolerance)) {
                return false;
            }
        }
        return true;
    }

    private ComparisonResult compareAsMultiset(
            List<Row> expected, List<Row> actual, BigDecimal tolerance) {
        List<Row> sortedExpected = sorted(expected);
        List<Row> sortedActual = sorted(actual);
        List<Row> missing = new ArrayList<>();
        List<Row> extra = new ArrayList<>();
        int e = 0;
        int a = 0;
        while (e < sortedExpected.size() && a < sortedActual.size()) {
            Row expectedRow = sortedExpected.get(e);
            Row actualRow = sortedActual.get(a);
            if (rowsMatch(expectedRow, actualRow, tolerance)) {
                e++;
                a++;
            } else if (RowOrdering.INSTANCE.compare(expectedRow, actualRow) < 0) {
                missing.add(expectedRow);
                e++;
            } else {
                extra.add(actualRow);
                a++;
            }
        }
        missing.addAll(sortedExpected.subList(e, sortedExpected.size()));
        extra.addAll(sortedActual.subList(a, sortedActual.size()));
        if (missing.isEmpty() && extra.isEmpty()) {
            return ComparisonResult.accepted();
        }
        return ComparisonResult.rowsDiffer(missing, extra);
    }

    private List<Row> sorted(List<Row> rows) {
        List<Row> copy = new ArrayList<>(rows);
        copy.sort(RowOrdering.INSTANCE);
        return copy;
    }

    private boolean rowsMatch(Row expected, Row actual, BigDecimal tolerance) {
        List<Cell> expectedCells = expected.cells();
        List<Cell> actualCells = actual.cells();
        for (int i = 0; i < expectedCells.size(); i++) {
            if (!cellsMatch(expectedCells.get(i), actualCells.get(i), tolerance)) {
                return false;
            }
        }
        return true;
    }

    private boolean cellsMatch(Cell expected, Cell actual, BigDecimal tolerance) {
        if (expected instanceof NumberCell left && actual instanceof NumberCell right) {
            return left.value().subtract(right.value()).abs().compareTo(tolerance) <= 0;
        }
        return expected.equals(actual);
    }
}
