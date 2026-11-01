package com.sqljudge.judge.sandbox;

import static com.sqljudge.judge.sandbox.SqlIdentifiers.quoted;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

final class PostgresDdl {

    private PostgresDdl() {}

    static String createTable(String schema, SnapshotTable table) {
        List<String> definitions = new ArrayList<>();
        for (SnapshotColumn column : table.columns()) {
            definitions.add(
                    quoted(column.name())
                            + " "
                            + typeOf(column.type())
                            + (column.isNullable() ? "" : " NOT NULL"));
        }
        List<String> keys =
                table.columns().stream()
                        .filter(SnapshotColumn::isPrimaryKey)
                        .map(column -> quoted(column.name()))
                        .toList();
        if (!keys.isEmpty()) {
            definitions.add("PRIMARY KEY (" + String.join(", ", keys) + ")");
        }
        return "CREATE TABLE "
                + qualified(schema, table.name())
                + " ("
                + String.join(", ", definitions)
                + ")";
    }

    static String insert(String schema, SnapshotTable table) {
        String columns =
                table.columns().stream()
                        .map(column -> quoted(column.name()))
                        .collect(Collectors.joining(", "));
        String placeholders = String.join(", ", Collections.nCopies(table.columns().size(), "?"));
        return "INSERT INTO "
                + qualified(schema, table.name())
                + " ("
                + columns
                + ") VALUES ("
                + placeholders
                + ")";
    }

    static String addForeignKey(String schema, String tableName, ForeignKey foreignKey) {
        return "ALTER TABLE "
                + qualified(schema, tableName)
                + " ADD FOREIGN KEY ("
                + quoted(foreignKey.column())
                + ") REFERENCES "
                + qualified(schema, foreignKey.referencedTable())
                + " ("
                + quoted(foreignKey.referencedColumn())
                + ")";
    }

    private static String qualified(String schema, String tableName) {
        return schema + "." + quoted(tableName);
    }

    private static String typeOf(LogicalType type) {
        return switch (type) {
            case INTEGER -> "BIGINT";
            case DECIMAL -> "NUMERIC";
            case TEXT -> "TEXT";
            case DATE -> "DATE";
            case TIMESTAMP -> "TIMESTAMP";
            case BOOLEAN -> "BOOLEAN";
        };
    }
}
