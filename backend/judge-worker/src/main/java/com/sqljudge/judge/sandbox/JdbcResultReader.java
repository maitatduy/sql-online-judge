package com.sqljudge.judge.sandbox;

import com.sqljudge.judge.compare.ResultTable;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class JdbcResultReader {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final String TOO_LARGE = "Kết quả vượt quá dung lượng cho phép";

    private JdbcResultReader() {}

    static ResultTable read(ResultSet resultSet, RunLimits limits)
            throws SQLException, ResultLimitExceededException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        List<String> columnNames = new ArrayList<>(columnCount);
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metadata.getColumnLabel(column));
        }
        List<List<Object>> rows = new ArrayList<>();
        long resultBytes = 0;
        while (resultSet.next()) {
            if (rows.size() == limits.maxRows()) {
                throw new ResultLimitExceededException(tooManyRowsMessage(limits.maxRows()));
            }
            List<Object> row = new ArrayList<>(columnCount);
            for (int column = 1; column <= columnCount; column++) {
                Object value = valueAt(resultSet, metadata, column);
                resultBytes += sizeOf(value);
                row.add(value);
            }
            if (resultBytes > limits.maxResultBytes()) {
                throw new ResultLimitExceededException(TOO_LARGE);
            }
            rows.add(row);
        }
        return new ResultTable(columnNames, rows);
    }

    private static Object valueAt(ResultSet resultSet, ResultSetMetaData metadata, int column)
            throws SQLException {
        boolean hasTimeZone = metadata.getColumnTypeName(column).endsWith("tz");
        return switch (metadata.getColumnType(column)) {
            case Types.DATE -> resultSet.getObject(column, LocalDate.class);
            case Types.TIME, Types.TIME_WITH_TIMEZONE ->
                    hasTimeZone
                            ? resultSet.getObject(column, OffsetTime.class)
                            : resultSet.getObject(column, LocalTime.class);
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE ->
                    hasTimeZone
                            ? resultSet.getObject(column, OffsetDateTime.class)
                            : resultSet.getObject(column, LocalDateTime.class);
            default -> resultSet.getObject(column);
        };
    }

    private static long sizeOf(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof byte[] bytes) {
            return bytes.length;
        }
        return value.toString().getBytes(StandardCharsets.UTF_8).length;
    }

    private static String tooManyRowsMessage(int maxRows) {
        return "Kết quả có hơn "
                + NumberFormat.getIntegerInstance(VIETNAMESE).format(maxRows)
                + " dòng";
    }
}
