package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaTableRequest;
import java.util.List;

public record SqlImportResponse(List<SchemaTableRequest> tables, List<TableData> data) {

    public SqlImportResponse {
        tables = List.copyOf(tables);
        data = List.copyOf(data);
    }

    public record TableData(String tableName, String csv, int rowCount) {}
}
