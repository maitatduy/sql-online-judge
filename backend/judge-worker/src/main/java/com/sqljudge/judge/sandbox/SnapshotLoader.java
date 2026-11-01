package com.sqljudge.judge.sandbox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

final class SnapshotLoader {

    private final DatasetDdl ddl;

    SnapshotLoader(DatasetDdl ddl) {
        this.ddl = ddl;
    }

    void load(Connection connection, String namespace, DatasetSnapshot dataset)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (SnapshotTable table : dataset.tables()) {
                statement.execute(
                        ddl.createTable(namespace, table, DatasetDdl.keyColumnsOf(dataset, table)));
                insertRows(connection, ddl.insert(namespace, table), table);
            }
            for (SnapshotTable table : dataset.tables()) {
                for (String foreignKey : ddl.foreignKeyStatements(namespace, table)) {
                    statement.execute(foreignKey);
                }
            }
        }
    }

    private void insertRows(Connection connection, String insertSql, SnapshotTable table)
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

    private void bind(PreparedStatement insert, int position, LogicalType type, String value)
            throws SQLException {
        if (value == null) {
            insert.setNull(position, sqlTypeOf(type));
            return;
        }
        insert.setObject(position, ddl.bindValue(type, value));
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
