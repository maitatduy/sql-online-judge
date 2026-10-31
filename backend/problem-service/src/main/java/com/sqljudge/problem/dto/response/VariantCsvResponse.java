package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.DatasetVariantTable;

public record VariantCsvResponse(String tableName, String csv, int rowCount) {

    public static VariantCsvResponse from(DatasetVariantTable table) {
        return new VariantCsvResponse(table.getTableName(), table.getCsv(), table.getRowCount());
    }
}
