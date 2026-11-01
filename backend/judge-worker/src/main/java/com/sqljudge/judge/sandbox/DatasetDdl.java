package com.sqljudge.judge.sandbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

abstract class DatasetDdl {

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

    String addForeignKey(String namespace, String tableName, ForeignKey foreignKey) {
        return "ALTER TABLE "
                + qualified(namespace, tableName)
                + " ADD FOREIGN KEY ("
                + quote(foreignKey.column())
                + ") REFERENCES "
                + qualified(namespace, foreignKey.referencedTable())
                + " ("
                + quote(foreignKey.referencedColumn())
                + ")";
    }

    String quotedNamespace(String namespace) {
        return quote(namespace);
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

    protected abstract String quote(String identifier);

    protected abstract String typeOf(LogicalType type, boolean isKeyColumn);

    String qualified(String namespace, String tableName) {
        return quote(namespace) + "." + quote(tableName);
    }
}
