package com.sqljudge.problem.service;

import com.sqljudge.problem.dto.response.ProblemDetailResponse.ColumnReferenceResponse;
import com.sqljudge.problem.dto.response.ProblemDetailResponse.SchemaColumnResponse;
import com.sqljudge.problem.dto.response.ProblemDetailResponse.SchemaTableResponse;
import com.sqljudge.problem.entity.DatasetColumn;
import com.sqljudge.problem.entity.DatasetTable;
import com.sqljudge.problem.entity.DatasetVersion;
import java.util.List;

public final class DatasetSchemas {

    private DatasetSchemas() {}

    public static List<SchemaTableResponse> tablesOf(DatasetVersion version) {
        return version.getTables().stream().map(DatasetSchemas::toTable).toList();
    }

    private static SchemaTableResponse toTable(DatasetTable table) {
        return new SchemaTableResponse(
                table.getName(),
                table.getColumns().stream().map(DatasetSchemas::toColumn).toList());
    }

    private static SchemaColumnResponse toColumn(DatasetColumn column) {
        ColumnReferenceResponse references =
                column.getReferencesTable() == null
                        ? null
                        : new ColumnReferenceResponse(
                                column.getReferencesTable(), column.getReferencesColumn());
        return new SchemaColumnResponse(
                column.getName(),
                column.getLogicalType().name(),
                column.isPrimaryKey(),
                column.isNullable(),
                references);
    }
}
