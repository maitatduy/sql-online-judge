package com.sqljudge.judge.compare;

import com.sqljudge.judge.compare.Cell.BooleanCell;
import com.sqljudge.judge.compare.Cell.BytesCell;
import com.sqljudge.judge.compare.Cell.NonFiniteNumberCell;
import com.sqljudge.judge.compare.Cell.NullCell;
import com.sqljudge.judge.compare.Cell.NumberCell;
import com.sqljudge.judge.compare.Cell.TemporalCell;
import com.sqljudge.judge.compare.Cell.TextCell;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

public final class ResultNormalizer {

    private static final NullCell NULL_CELL = new NullCell();

    public NormalizedTable normalize(ResultTable raw) {
        List<Row> rows = raw.rows().stream().map(this::normalizeRow).toList();
        return new NormalizedTable(raw.columnNames(), rows);
    }

    private Row normalizeRow(List<Object> values) {
        return new Row(values.stream().map(this::normalizeValue).toList());
    }

    private Cell normalizeValue(Object value) {
        if (value == null) {
            return NULL_CELL;
        }
        if (value instanceof Double || value instanceof Float) {
            return floatingPoint(((Number) value).doubleValue(), value.toString());
        }
        if (value instanceof Number number) {
            return new NumberCell(exactDecimal(number));
        }
        if (value instanceof String || value instanceof Character || value instanceof UUID) {
            return new TextCell(value.toString());
        }
        if (value instanceof Boolean flag) {
            return new BooleanCell(flag);
        }
        if (value instanceof byte[] bytes) {
            return new BytesCell(HexFormat.of().formatHex(bytes));
        }
        return normalizeTemporal(value);
    }

    private BigDecimal exactDecimal(Number number) {
        if (number instanceof BigDecimal decimal) {
            return decimal;
        }
        if (number instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }
        if (number instanceof Integer
                || number instanceof Long
                || number instanceof Short
                || number instanceof Byte) {
            return BigDecimal.valueOf(number.longValue());
        }
        throw unsupported(number);
    }

    private Cell floatingPoint(double number, String shortestForm) {
        if (Double.isFinite(number)) {
            return new NumberCell(new BigDecimal(shortestForm));
        }
        return new NonFiniteNumberCell(shortestForm);
    }

    private Cell normalizeTemporal(Object value) {
        if (value instanceof Instant instant) {
            return new TemporalCell(TemporalKind.TIMESTAMP_UTC, instant.toString());
        }
        if (value instanceof OffsetDateTime dateTime) {
            return normalizeTemporal(dateTime.toInstant());
        }
        if (value instanceof ZonedDateTime dateTime) {
            return normalizeTemporal(dateTime.toInstant());
        }
        if (value instanceof OffsetTime time) {
            String utc = time.withOffsetSameInstant(ZoneOffset.UTC).toString();
            return new TemporalCell(TemporalKind.TIME_UTC, utc);
        }
        if (value instanceof LocalDateTime dateTime) {
            return new TemporalCell(TemporalKind.TIMESTAMP, dateTime.toString());
        }
        if (value instanceof LocalDate date) {
            return new TemporalCell(TemporalKind.DATE, date.toString());
        }
        if (value instanceof LocalTime time) {
            return new TemporalCell(TemporalKind.TIME, time.toString());
        }
        throw unsupported(value);
    }

    private IllegalArgumentException unsupported(Object value) {
        return new IllegalArgumentException(
                "Unsupported result value type: " + value.getClass().getName());
    }
}
