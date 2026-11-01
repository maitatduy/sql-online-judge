package com.sqljudge.judge.sandbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record SnapshotTable(
        String name,
        List<SnapshotColumn> columns,
        List<ForeignKey> foreignKeys,
        List<List<String>> rows) {

    public SnapshotTable {
        SqlIdentifiers.requireValid(name);
        columns = List.copyOf(columns);
        foreignKeys = List.copyOf(foreignKeys);
        int width = columns.size();
        List<List<String>> copiedRows = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            if (row.size() != width) {
                throw new IllegalArgumentException("Row width does not match table " + name);
            }
            copiedRows.add(Collections.unmodifiableList(new ArrayList<>(row)));
        }
        rows = Collections.unmodifiableList(copiedRows);
    }
}
