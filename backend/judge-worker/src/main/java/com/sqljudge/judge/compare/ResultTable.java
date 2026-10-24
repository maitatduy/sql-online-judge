package com.sqljudge.judge.compare;

import java.util.List;

public record ResultTable(List<String> columnNames, List<List<Object>> rows) {

    public ResultTable {
        columnNames = List.copyOf(columnNames);
        rows = List.copyOf(rows);
    }
}
