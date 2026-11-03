package com.sqljudge.judge.job;

import com.sqljudge.judge.compare.Cell;
import com.sqljudge.judge.compare.Cell.BytesCell;
import com.sqljudge.judge.compare.Cell.NonFiniteNumberCell;
import com.sqljudge.judge.compare.Cell.NumberCell;
import com.sqljudge.judge.compare.Cell.TemporalCell;
import com.sqljudge.judge.compare.Cell.TextCell;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.Row;
import java.util.List;

public record ExpectedTable(List<String> columns, List<List<WireCell>> rows) {

    public ExpectedTable {
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
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

    public record WireCell(CellKind kind, String value) {}

    static ExpectedTable of(NormalizedTable table) {
        List<List<WireCell>> rows = table.rows().stream().map(ExpectedTable::wireRow).toList();
        return new ExpectedTable(table.columnNames(), rows);
    }

    private static List<WireCell> wireRow(Row row) {
        return row.cells().stream().map(ExpectedTable::wireCell).toList();
    }

    private static WireCell wireCell(Cell cell) {
        if (cell instanceof NumberCell number) {
            return new WireCell(CellKind.NUMBER, number.value().toPlainString());
        }
        if (cell instanceof NonFiniteNumberCell number) {
            return new WireCell(CellKind.NON_FINITE_NUMBER, number.value());
        }
        if (cell instanceof TextCell text) {
            return new WireCell(CellKind.TEXT, text.value());
        }
        if (cell instanceof TemporalCell temporal) {
            return new WireCell(CellKind.valueOf(temporal.kind().name()), temporal.iso());
        }
        if (cell instanceof BytesCell bytes) {
            return new WireCell(CellKind.BYTES, bytes.hex());
        }
        return new WireCell(CellKind.NULL, null);
    }
}
