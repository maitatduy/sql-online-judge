package com.sqljudge.problem.service;

import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.ColumnReferenceRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaColumnRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaTableRequest;
import com.sqljudge.problem.entity.LogicalType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;

final class SqlImportSchema {

    private static final Pattern INLINE_REFERENCE =
            Pattern.compile(
                    "REFERENCES\\s+([\\w`\"\\[\\].]+)\\s*\\(\\s*([\\w`\"\\[\\]]+)\\s*\\)",
                    Pattern.CASE_INSENSITIVE);
    private static final Map<String, LogicalType> TYPES = typeMap();

    private SqlImportSchema() {}

    static SchemaTableRequest tableOf(CreateTable createTable, String prefix) {
        if (createTable.getSelect() != null) {
            throw SqlImportParser.invalid(
                    prefix + "CREATE TABLE phải liệt kê cột, không dùng AS SELECT");
        }
        String tableName = SqlImportParser.unquote(createTable.getTable().getName());
        Set<String> primaryKeys = new HashSet<>();
        Map<String, ColumnReferenceRequest> references = new HashMap<>();
        readTableLevelKeys(createTable.getIndexes(), primaryKeys, references);
        List<ColumnDefinition> definitions =
                createTable.getColumnDefinitions() == null
                        ? List.of()
                        : createTable.getColumnDefinitions();
        List<SchemaColumnRequest> columns =
                definitions.stream()
                        .map(definition -> columnOf(definition, primaryKeys, references, prefix))
                        .toList();
        return new SchemaTableRequest(tableName, columns);
    }

    private static void readTableLevelKeys(
            List<Index> indexes,
            Set<String> primaryKeys,
            Map<String, ColumnReferenceRequest> references) {
        if (indexes == null) {
            return;
        }
        for (Index index : indexes) {
            if (index instanceof ForeignKeyIndex foreignKey) {
                List<String> columns = foreignKey.getColumnsNames();
                List<String> targets = foreignKey.getReferencedColumnNames();
                for (int position = 0; position < columns.size(); position++) {
                    references.put(
                            SqlImportParser.unquote(columns.get(position)),
                            new ColumnReferenceRequest(
                                    SqlImportParser.unquote(foreignKey.getTable().getName()),
                                    SqlImportParser.unquote(targets.get(position))));
                }
            } else if ("PRIMARY KEY".equalsIgnoreCase(index.getType())) {
                index.getColumnsNames()
                        .forEach(column -> primaryKeys.add(SqlImportParser.unquote(column)));
            }
        }
    }

    private static SchemaColumnRequest columnOf(
            ColumnDefinition definition,
            Set<String> primaryKeys,
            Map<String, ColumnReferenceRequest> references,
            String prefix) {
        String name = SqlImportParser.unquote(definition.getColumnName());
        String specs =
                definition.getColumnSpecs() == null
                        ? ""
                        : String.join(" ", definition.getColumnSpecs());
        String upperSpecs = specs.toUpperCase(Locale.ROOT);
        boolean isPrimaryKey = primaryKeys.contains(name) || upperSpecs.contains("PRIMARY KEY");
        boolean isNotNull = upperSpecs.contains("NOT NULL");
        return new SchemaColumnRequest(
                name,
                typeOf(definition.getColDataType().getDataType(), name, prefix),
                isPrimaryKey,
                !isPrimaryKey && !isNotNull,
                references.getOrDefault(name, inlineReference(specs)));
    }

    private static ColumnReferenceRequest inlineReference(String specs) {
        Matcher matcher = INLINE_REFERENCE.matcher(specs);
        return matcher.find()
                ? new ColumnReferenceRequest(
                        SqlImportParser.unquote(matcher.group(1)),
                        SqlImportParser.unquote(matcher.group(2)))
                : null;
    }

    private static LogicalType typeOf(String dataType, String column, String prefix) {
        String baseType = dataType.trim().toUpperCase(Locale.ROOT).split("[\\s(]")[0];
        LogicalType type = TYPES.get(baseType);
        if (type == null) {
            throw SqlImportParser.invalid(
                    prefix + "kiểu " + baseType + " của cột " + column + " chưa hỗ trợ");
        }
        return type;
    }

    private static Map<String, LogicalType> typeMap() {
        Map<String, LogicalType> types = new HashMap<>();
        List.of(
                        "INT",
                        "INTEGER",
                        "BIGINT",
                        "SMALLINT",
                        "TINYINT",
                        "MEDIUMINT",
                        "SERIAL",
                        "BIGSERIAL",
                        "SMALLSERIAL",
                        "INT2",
                        "INT4",
                        "INT8")
                .forEach(name -> types.put(name, LogicalType.INTEGER));
        List.of("DECIMAL", "NUMERIC", "REAL", "FLOAT", "FLOAT4", "FLOAT8", "DOUBLE", "MONEY")
                .forEach(name -> types.put(name, LogicalType.DECIMAL));
        List.of(
                        "VARCHAR",
                        "CHAR",
                        "CHARACTER",
                        "NVARCHAR",
                        "NCHAR",
                        "TEXT",
                        "TINYTEXT",
                        "MEDIUMTEXT",
                        "LONGTEXT",
                        "CLOB",
                        "STRING",
                        "VARCHAR2")
                .forEach(name -> types.put(name, LogicalType.TEXT));
        types.put("DATE", LogicalType.DATE);
        List.of("TIMESTAMP", "TIMESTAMPTZ", "DATETIME")
                .forEach(name -> types.put(name, LogicalType.TIMESTAMP));
        List.of("BOOLEAN", "BOOL").forEach(name -> types.put(name, LogicalType.BOOLEAN));
        return Map.copyOf(types);
    }
}
