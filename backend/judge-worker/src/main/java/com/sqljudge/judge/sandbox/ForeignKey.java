package com.sqljudge.judge.sandbox;

public record ForeignKey(String column, String referencedTable, String referencedColumn) {

    public ForeignKey {
        SqlIdentifiers.requireValid(column);
        SqlIdentifiers.requireValid(referencedTable);
        SqlIdentifiers.requireValid(referencedColumn);
    }
}
