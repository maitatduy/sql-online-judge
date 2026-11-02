package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.LogicalType;
import java.util.List;
import java.util.UUID;

public record InternalDatasetSnapshotResponse(
        UUID versionId,
        String checksum,
        List<SnapshotTableResponse> tables,
        List<SnapshotVariantResponse> variants) {

    public record SnapshotTableResponse(String name, List<SnapshotColumnResponse> columns) {}

    public record SnapshotColumnResponse(
            String name,
            LogicalType type,
            boolean isPrimaryKey,
            boolean isNullable,
            ColumnReferenceResponse references) {}

    public record ColumnReferenceResponse(String table, String column) {}

    public record SnapshotVariantResponse(
            UUID id, String name, boolean isPublic, List<SnapshotVariantTableResponse> tables) {}

    public record SnapshotVariantTableResponse(String tableName, List<List<String>> rows) {}
}
