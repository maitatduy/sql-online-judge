package com.sqljudge.problem.service;

import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.ColumnReferenceRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaColumnRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaTableRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class DatasetSchemaValidator {

    private static final Set<String> RESERVED_WORDS =
            Set.of(
                    "all",
                    "alter",
                    "and",
                    "as",
                    "asc",
                    "between",
                    "by",
                    "case",
                    "check",
                    "column",
                    "constraint",
                    "create",
                    "default",
                    "delete",
                    "desc",
                    "distinct",
                    "drop",
                    "else",
                    "end",
                    "exists",
                    "foreign",
                    "from",
                    "grant",
                    "group",
                    "having",
                    "in",
                    "index",
                    "insert",
                    "into",
                    "is",
                    "join",
                    "key",
                    "like",
                    "limit",
                    "not",
                    "null",
                    "offset",
                    "on",
                    "or",
                    "order",
                    "primary",
                    "rank",
                    "references",
                    "row",
                    "rows",
                    "select",
                    "table",
                    "then",
                    "union",
                    "update",
                    "user",
                    "values",
                    "when",
                    "where",
                    "window",
                    "with");
    private static final String RESERVED_MESSAGE = "Tên này trùng từ khoá SQL";
    private static final String REFERENCE_TARGET_MESSAGE =
            "Khoá ngoại phải trỏ tới cột khoá chính của một bảng trong schema";

    private DatasetSchemaValidator() {}

    public static void validate(UpdateDatasetSchemaRequest schema) {
        List<SchemaTableRequest> tables = schema.tables();
        Set<String> tableNames = new HashSet<>();
        for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
            SchemaTableRequest table = tables.get(tableIndex);
            String tablePath = "tables[" + tableIndex + "]";
            requireNotReserved(table.name(), tablePath + ".name");
            if (!tableNames.add(table.name())) {
                throw new InvalidFieldException(tablePath + ".name", "Tên bảng bị trùng");
            }
            validateColumns(table.columns(), tablePath);
        }
        for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
            validateReferences(tables, tableIndex);
        }
    }

    private static void validateColumns(List<SchemaColumnRequest> columns, String tablePath) {
        Set<String> columnNames = new HashSet<>();
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            SchemaColumnRequest column = columns.get(columnIndex);
            String columnPath = tablePath + ".columns[" + columnIndex + "]";
            requireNotReserved(column.name(), columnPath + ".name");
            if (!columnNames.add(column.name())) {
                throw new InvalidFieldException(
                        columnPath + ".name", "Tên cột bị trùng trong bảng");
            }
            if (column.isPrimaryKey() && column.isNullable()) {
                throw new InvalidFieldException(
                        columnPath + ".isNullable", "Cột khoá chính không được để trống");
            }
        }
    }

    private static void validateReferences(List<SchemaTableRequest> tables, int tableIndex) {
        List<SchemaColumnRequest> columns = tables.get(tableIndex).columns();
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            SchemaColumnRequest column = columns.get(columnIndex);
            if (column.references() == null) {
                continue;
            }
            String path = "tables[" + tableIndex + "].columns[" + columnIndex + "].references";
            SchemaColumnRequest target =
                    primaryKeyTarget(tables, column.references())
                            .orElseThrow(
                                    () ->
                                            new InvalidFieldException(
                                                    path, REFERENCE_TARGET_MESSAGE));
            if (target.type() != column.type()) {
                throw new InvalidFieldException(
                        path, "Khoá ngoại phải cùng kiểu với cột được trỏ tới");
            }
        }
    }

    private static Optional<SchemaColumnRequest> primaryKeyTarget(
            List<SchemaTableRequest> tables, ColumnReferenceRequest reference) {
        return tables.stream()
                .filter(table -> table.name().equals(reference.table()))
                .flatMap(table -> table.columns().stream())
                .filter(column -> column.name().equals(reference.column()))
                .filter(SchemaColumnRequest::isPrimaryKey)
                .findFirst();
    }

    private static void requireNotReserved(String name, String path) {
        if (RESERVED_WORDS.contains(name)) {
            throw new InvalidFieldException(path, RESERVED_MESSAGE);
        }
    }
}
