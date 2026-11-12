package com.sqljudge.judge.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sqljudge.judge.compare.Cell;
import com.sqljudge.judge.compare.Cell.BytesCell;
import com.sqljudge.judge.compare.Cell.NonFiniteNumberCell;
import com.sqljudge.judge.compare.Cell.NullCell;
import com.sqljudge.judge.compare.Cell.NumberCell;
import com.sqljudge.judge.compare.Cell.TemporalCell;
import com.sqljudge.judge.compare.Cell.TextCell;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.Row;
import com.sqljudge.judge.compare.TemporalKind;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ExpectedTables {

    private ExpectedTables() {}

    public static NormalizedTable decode(List<String> columns, JsonNode rows) {
        List<Row> decoded = new ArrayList<>();
        for (JsonNode row : rows) {
            List<Cell> cells = new ArrayList<>();
            row.forEach(cell -> cells.add(cellOf(cell)));
            decoded.add(new Row(cells));
        }
        return new NormalizedTable(columns, decoded);
    }

    private static Cell cellOf(JsonNode cell) {
        if (!cell.hasNonNull("kind")) {
            throw new IllegalArgumentException("Expected result cell has no kind");
        }
        String kind = cell.get("kind").asText();
        String value = cell.path("value").isNull() ? null : cell.path("value").asText();
        return switch (kind) {
            case "NUMBER" -> new NumberCell(new BigDecimal(value));
            case "NON_FINITE_NUMBER" -> new NonFiniteNumberCell(value);
            case "TEXT" -> new TextCell(value);
            case "BYTES" -> new BytesCell(value);
            case "NULL" -> new NullCell();
            default -> new TemporalCell(TemporalKind.valueOf(kind), value);
        };
    }
}
