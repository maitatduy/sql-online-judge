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
import com.sqljudge.judge.compare.ResultNormalizer;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.compare.Row;
import com.sqljudge.judge.compare.TemporalKind;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class ExpectedTables {

    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final ResultNormalizer NORMALIZER = new ResultNormalizer();

    private ExpectedTables() {}

    public static NormalizedTable decode(List<String> columns, JsonNode rows) {
        boolean isWireFormat = cellsOf(rows).anyMatch(JsonNode::isObject);
        if (isWireFormat) {
            return new NormalizedTable(columns, wireRows(rows));
        }
        return NORMALIZER.normalize(new ResultTable(columns, legacyRows(rows)));
    }

    private static Stream<JsonNode> cellsOf(JsonNode rows) {
        return StreamSupport.stream(rows.spliterator(), false)
                .flatMap(row -> StreamSupport.stream(row.spliterator(), false));
    }

    private static List<Row> wireRows(JsonNode rows) {
        List<Row> decoded = new ArrayList<>();
        for (JsonNode row : rows) {
            List<Cell> cells = new ArrayList<>();
            row.forEach(cell -> cells.add(wireCell(cell)));
            decoded.add(new Row(cells));
        }
        return decoded;
    }

    private static Cell wireCell(JsonNode cell) {
        String kind = cell.path("kind").asText();
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

    private static List<List<Object>> legacyRows(JsonNode rows) {
        List<List<Object>> decoded = new ArrayList<>();
        for (JsonNode row : rows) {
            List<Object> values = new ArrayList<>();
            row.forEach(cell -> values.add(legacyValue(cell)));
            decoded.add(values);
        }
        return decoded;
    }

    private static Object legacyValue(JsonNode cell) {
        if (cell.isNull()) {
            return null;
        }
        if (cell.isNumber()) {
            return cell.decimalValue();
        }
        String text = cell.asText();
        return DECIMAL.matcher(text).matches() ? new BigDecimal(text) : text;
    }
}
