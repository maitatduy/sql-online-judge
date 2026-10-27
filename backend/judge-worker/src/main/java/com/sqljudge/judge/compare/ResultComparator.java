package com.sqljudge.judge.compare;

import com.sqljudge.judge.compare.Cell.NumberCell;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public final class ResultComparator {

    public ComparisonResult compare(
            NormalizedTable expected, NormalizedTable actual, CompareOptions options) {
        if (expected.columnCount() != actual.columnCount()) {
            return ComparisonResult.columnsDiffer(MismatchReason.COLUMN_COUNT);
        }
        if (options.compareColumnNames()
                && !sameColumnNames(expected.columnNames(), actual.columnNames())) {
            return ComparisonResult.columnsDiffer(MismatchReason.COLUMN_NAMES);
        }
        BigDecimal tolerance = options.tolerance();
        if (options.ordered()) {
            return compareInOrder(expected.rows(), actual.rows(), tolerance);
        }
        return compareAsMultiset(expected.rows(), actual.rows(), tolerance);
    }

    private boolean sameColumnNames(List<String> expected, List<String> actual) {
        return IntStream.range(0, expected.size())
                .allMatch(i -> expected.get(i).equalsIgnoreCase(actual.get(i)));
    }

    private ComparisonResult compareInOrder(
            List<Row> expected, List<Row> actual, BigDecimal tolerance) {
        int firstMismatch = firstMismatchInSequence(expected, actual, tolerance);
        if (firstMismatch < 0) {
            return ComparisonResult.accepted();
        }
        ComparisonResult asMultiset = compareAsMultiset(expected, actual, tolerance);
        if (asMultiset.isAccepted()) {
            return ComparisonResult.orderMismatch(firstMismatch);
        }
        return asMultiset;
    }

    private int firstMismatchInSequence(
            List<Row> expected, List<Row> actual, BigDecimal tolerance) {
        int shared = Math.min(expected.size(), actual.size());
        for (int i = 0; i < shared; i++) {
            if (!rowsMatch(expected.get(i), actual.get(i), tolerance)) {
                return i;
            }
        }
        return expected.size() == actual.size() ? -1 : shared;
    }

    private ComparisonResult compareAsMultiset(
            List<Row> expected, List<Row> actual, BigDecimal tolerance) {
        List<Integer> sortedExpected = sortedIndices(expected);
        List<Integer> sortedActual = sortedIndices(actual);
        List<Integer> missing = new ArrayList<>();
        List<Integer> extra = new ArrayList<>();
        int e = 0;
        int a = 0;
        while (e < sortedExpected.size() && a < sortedActual.size()) {
            Row expectedRow = expected.get(sortedExpected.get(e));
            Row actualRow = actual.get(sortedActual.get(a));
            if (rowsMatch(expectedRow, actualRow, tolerance)) {
                e++;
                a++;
            } else if (RowOrdering.INSTANCE.compare(expectedRow, actualRow) < 0) {
                missing.add(sortedExpected.get(e++));
            } else {
                extra.add(sortedActual.get(a++));
            }
        }
        missing.addAll(sortedExpected.subList(e, sortedExpected.size()));
        extra.addAll(sortedActual.subList(a, sortedActual.size()));
        if (missing.isEmpty() && extra.isEmpty()) {
            return ComparisonResult.accepted();
        }
        missing.sort(Comparator.naturalOrder());
        extra.sort(Comparator.naturalOrder());
        return ComparisonResult.rowsDiffer(missing, extra);
    }

    private List<Integer> sortedIndices(List<Row> rows) {
        return IntStream.range(0, rows.size())
                .boxed()
                .sorted(Comparator.comparing(rows::get, RowOrdering.INSTANCE))
                .toList();
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
            BigDecimal magnitude = BigDecimal.ONE.max(left.value().abs()).max(right.value().abs());
            BigDecimal difference = left.value().subtract(right.value()).abs();
            return difference.compareTo(tolerance.multiply(magnitude)) <= 0;
        }
        return expected.equals(actual);
    }
}
