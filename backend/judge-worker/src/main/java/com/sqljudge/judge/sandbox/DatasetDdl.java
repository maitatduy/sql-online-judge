package com.sqljudge.judge.sandbox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

abstract class DatasetDdl {

    private static final int DATE_LENGTH = 10;
    private static final Set<String> TRUE_VALUES = Set.of("true", "1");

    String createTable(String namespace, SnapshotTable table, Set<String> keyColumns) {
        List<String> definitions = new ArrayList<>();
        for (SnapshotColumn column : table.columns()) {
            definitions.add(
                    quote(column.name())
                            + " "
                            + typeOf(column.type(), keyColumns.contains(column.name()))
                            + (column.isNullable() ? "" : " NOT NULL"));
        }
        List<String> primaryKey =
                table.columns().stream()
                        .filter(SnapshotColumn::isPrimaryKey)
                        .map(column -> quote(column.name()))
                        .toList();
        if (!primaryKey.isEmpty()) {
            definitions.add("PRIMARY KEY (" + String.join(", ", primaryKey) + ")");
        }
        definitions.addAll(inlineConstraints(table));
        return "CREATE TABLE "
                + qualified(namespace, table.name())
                + " ("
                + String.join(", ", definitions)
                + ")";
    }

    String insert(String namespace, SnapshotTable table) {
        String columns =
                table.columns().stream()
                        .map(column -> quote(column.name()))
                        .collect(Collectors.joining(", "));
        String placeholders = String.join(", ", Collections.nCopies(table.columns().size(), "?"));
        return "INSERT INTO "
                + qualified(namespace, table.name())
                + " ("
                + columns
                + ") VALUES ("
                + placeholders
                + ")";
    }

    List<String> foreignKeyStatements(String namespace, SnapshotTable table) {
        return table.foreignKeys().stream()
                .map(
                        foreignKey ->
                                "ALTER TABLE "
                                        + qualified(namespace, table.name())
                                        + " ADD FOREIGN KEY ("
                                        + quote(foreignKey.column())
                                        + ") REFERENCES "
                                        + qualified(namespace, foreignKey.referencedTable())
                                        + " ("
                                        + quote(foreignKey.referencedColumn())
                                        + ")")
                .toList();
    }

    Object bindValue(LogicalType type, String value) {
        return switch (type) {
            case INTEGER -> Long.valueOf(value);
            case DECIMAL -> new BigDecimal(value);
            case TEXT -> value;
            case DATE -> LocalDate.parse(value);
            case TIMESTAMP -> LocalDateTime.parse(isoTimestamp(value));
            case BOOLEAN -> TRUE_VALUES.contains(value.toLowerCase(Locale.ROOT));
        };
    }

    String quotedNamespace(String namespace) {
        return quote(namespace);
    }

    String qualified(String namespace, String tableName) {
        return quote(namespace) + "." + quote(tableName);
    }

    static Set<String> keyColumnsOf(DatasetSnapshot dataset, SnapshotTable table) {
        Set<String> keyColumns = new HashSet<>();
        table.columns().stream()
                .filter(SnapshotColumn::isPrimaryKey)
                .forEach(column -> keyColumns.add(column.name()));
        table.foreignKeys().forEach(foreignKey -> keyColumns.add(foreignKey.column()));
        for (SnapshotTable other : dataset.tables()) {
            other.foreignKeys().stream()
                    .filter(foreignKey -> foreignKey.referencedTable().equals(table.name()))
                    .forEach(foreignKey -> keyColumns.add(foreignKey.referencedColumn()));
        }
        return keyColumns;
    }

    static String isoTimestamp(String value) {
        return value.length() > DATE_LENGTH && value.charAt(DATE_LENGTH) == ' '
                ? value.substring(0, DATE_LENGTH) + "T" + value.substring(DATE_LENGTH + 1)
                : value;
    }

    protected List<String> inlineConstraints(SnapshotTable table) {
        return List.of();
    }

    protected abstract String quote(String identifier);

    protected abstract String typeOf(LogicalType type, boolean isKeyColumn);
}
