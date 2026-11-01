package com.sqljudge.judge.sandbox;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class SnapshotLoader {

    private static final int DATE_LENGTH = 10;
    private static final Set<String> TRUE_VALUES = Set.of("true", "1");

    private SnapshotLoader() {}

    static void insertRows(Connection connection, String insertSql, SnapshotTable table)
            throws SQLException {
        if (table.rows().isEmpty()) {
            return;
        }
        List<SnapshotColumn> columns = table.columns();
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            for (List<String> row : table.rows()) {
                for (int index = 0; index < columns.size(); index++) {
                    bind(insert, index + 1, columns.get(index).type(), row.get(index));
                }
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static void bind(PreparedStatement insert, int position, LogicalType type, String value)
            throws SQLException {
        if (value == null) {
            insert.setNull(position, sqlTypeOf(type));
            return;
        }
        insert.setObject(position, valueOf(type, value));
    }

    private static Object valueOf(LogicalType type, String value) {
        return switch (type) {
            case INTEGER -> Long.valueOf(value);
            case DECIMAL -> new BigDecimal(value);
            case TEXT -> value;
            case DATE -> LocalDate.parse(value);
            case TIMESTAMP -> LocalDateTime.parse(isoTimestamp(value));
            case BOOLEAN -> TRUE_VALUES.contains(value.toLowerCase(Locale.ROOT));
        };
    }

    private static String isoTimestamp(String value) {
        return value.length() > DATE_LENGTH && value.charAt(DATE_LENGTH) == ' '
                ? value.substring(0, DATE_LENGTH) + "T" + value.substring(DATE_LENGTH + 1)
                : value;
    }

    private static int sqlTypeOf(LogicalType type) {
        return switch (type) {
            case INTEGER -> Types.BIGINT;
            case DECIMAL -> Types.NUMERIC;
            case TEXT -> Types.VARCHAR;
            case DATE -> Types.DATE;
            case TIMESTAMP -> Types.TIMESTAMP;
            case BOOLEAN -> Types.BOOLEAN;
        };
    }
}
