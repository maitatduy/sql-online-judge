package com.sqljudge.submission.entity;

import java.util.List;

public record ResultDiff(
        MismatchReason reason,
        List<String> expectedColumns,
        List<String> actualColumns,
        int expectedRowCount,
        int actualRowCount,
        List<List<DiffCell>> missingRows,
        List<List<DiffCell>> extraRows,
        OrderMismatch orderMismatch) {

    public enum MismatchReason {
        NONE,
        COLUMN_COUNT,
        COLUMN_NAMES,
        ROWS,
        ORDER
    }

    public enum CellKind {
        NULL,
        NUMBER,
        NON_FINITE_NUMBER,
        TEXT,
        DATE,
        TIME,
        TIME_UTC,
        TIMESTAMP,
        TIMESTAMP_UTC,
        BYTES
    }

    public record DiffCell(CellKind kind, String value) {}

    public record OrderMismatch(int rowNumber, List<DiffCell> expected, List<DiffCell> actual) {}
}
