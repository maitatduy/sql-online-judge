package com.sqljudge.judge.sandbox;

final class MysqlDdl extends DatasetDdl {

    @Override
    protected String quote(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    protected String typeOf(LogicalType type, boolean isKeyColumn) {
        return switch (type) {
            case INTEGER -> "BIGINT";
            case DECIMAL -> "DECIMAL(38, 10)";
            case TEXT -> isKeyColumn ? "VARCHAR(255)" : "TEXT";
            case DATE -> "DATE";
            case TIMESTAMP -> "DATETIME(6)";
            case BOOLEAN -> "BOOLEAN";
        };
    }
}
