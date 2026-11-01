package com.sqljudge.judge.sandbox;

final class PostgresDdl extends DatasetDdl {

    @Override
    protected String quote(String identifier) {
        return "\"" + identifier + "\"";
    }

    @Override
    protected String typeOf(LogicalType type, boolean isKeyColumn) {
        return switch (type) {
            case INTEGER -> "BIGINT";
            case DECIMAL -> "NUMERIC";
            case TEXT -> "TEXT";
            case DATE -> "DATE";
            case TIMESTAMP -> "TIMESTAMP";
            case BOOLEAN -> "BOOLEAN";
        };
    }
}
