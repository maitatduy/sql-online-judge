package com.sqljudge.judge.compare;

import java.util.List;

public record NormalizedTable(List<String> columnNames, List<Row> rows) {

    public NormalizedTable {
        columnNames = List.copyOf(columnNames);
        rows = List.copyOf(rows);
    }

    public int columnCount() {
        return columnNames.size();
    }
}
