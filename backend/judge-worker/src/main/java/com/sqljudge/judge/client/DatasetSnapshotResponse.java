package com.sqljudge.judge.client;

import com.sqljudge.judge.sandbox.LogicalType;
import java.util.List;
import java.util.UUID;

public record DatasetSnapshotResponse(
        UUID versionId,
        String checksum,
        List<SnapshotTable> tables,
        List<SnapshotVariant> variants) {

    public record SnapshotTable(String name, List<SnapshotColumn> columns) {}

    public record SnapshotColumn(
            String name,
            LogicalType type,
            boolean isPrimaryKey,
            boolean isNullable,
            ColumnReference references) {}

    public record ColumnReference(String table, String column) {}

    public record SnapshotVariant(
            UUID id, String name, boolean isPublic, List<SnapshotVariantTable> tables) {}

    public record SnapshotVariantTable(String tableName, List<List<String>> rows) {}
}
