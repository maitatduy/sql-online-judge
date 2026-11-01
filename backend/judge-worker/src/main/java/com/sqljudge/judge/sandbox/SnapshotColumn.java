package com.sqljudge.judge.sandbox;

public record SnapshotColumn(
        String name, LogicalType type, boolean isPrimaryKey, boolean isNullable) {

    public SnapshotColumn {
        SqlIdentifiers.requireValid(name);
    }
}
