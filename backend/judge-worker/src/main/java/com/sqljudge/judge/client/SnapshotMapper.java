package com.sqljudge.judge.client;

import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariant;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariantTable;
import com.sqljudge.judge.sandbox.DatasetSnapshot;
import com.sqljudge.judge.sandbox.ForeignKey;
import com.sqljudge.judge.sandbox.SnapshotColumn;
import com.sqljudge.judge.sandbox.SnapshotTable;
import java.util.List;

public final class SnapshotMapper {

    private SnapshotMapper() {}

    public static DatasetSnapshot toDataset(
            DatasetSnapshotResponse snapshot, SnapshotVariant variant) {
        return new DatasetSnapshot(
                snapshot.tables().stream().map(table -> tableOf(table, variant)).toList());
    }

    private static SnapshotTable tableOf(
            DatasetSnapshotResponse.SnapshotTable table, SnapshotVariant variant) {
        List<SnapshotColumn> columns =
                table.columns().stream()
                        .map(
                                column ->
                                        new SnapshotColumn(
                                                column.name(),
                                                column.type(),
                                                column.isPrimaryKey(),
                                                column.isNullable()))
                        .toList();
        List<ForeignKey> foreignKeys =
                table.columns().stream()
                        .filter(column -> column.references() != null)
                        .map(
                                column ->
                                        new ForeignKey(
                                                column.name(),
                                                column.references().table(),
                                                column.references().column()))
                        .toList();
        return new SnapshotTable(table.name(), columns, foreignKeys, rowsOf(variant, table.name()));
    }

    private static List<List<String>> rowsOf(SnapshotVariant variant, String tableName) {
        return variant.tables().stream()
                .filter(data -> data.tableName().equals(tableName))
                .findFirst()
                .map(SnapshotVariantTable::rows)
                .orElse(List.of());
    }
}
