package com.sqljudge.judge.sandbox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class SqliteDdl extends DatasetDdl {

    private static final DateTimeFormatter TIMESTAMP_TEXT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    List<String> foreignKeyStatements(String namespace, SnapshotTable table) {
        return List.of();
    }

    @Override
    Object bindValue(LogicalType type, String value) {
        return switch (type) {
            case DATE -> LocalDate.parse(value).toString();
            case TIMESTAMP -> LocalDateTime.parse(isoTimestamp(value)).format(TIMESTAMP_TEXT);
            case BOOLEAN -> Boolean.TRUE.equals(super.bindValue(type, value)) ? 1 : 0;
            default -> super.bindValue(type, value);
        };
    }

    @Override
    protected List<String> inlineConstraints(SnapshotTable table) {
        return table.foreignKeys().stream()
                .map(
                        foreignKey ->
                                "FOREIGN KEY ("
                                        + quote(foreignKey.column())
                                        + ") REFERENCES "
                                        + quote(foreignKey.referencedTable())
                                        + " ("
                                        + quote(foreignKey.referencedColumn())
                                        + ")")
                .toList();
    }

    @Override
    protected String quote(String identifier) {
        return "\"" + identifier + "\"";
    }

    @Override
    protected String typeOf(LogicalType type, boolean isKeyColumn) {
        return switch (type) {
            case INTEGER, BOOLEAN -> "INTEGER";
            case DECIMAL -> "NUMERIC";
            case TEXT, DATE, TIMESTAMP -> "TEXT";
        };
    }
}
