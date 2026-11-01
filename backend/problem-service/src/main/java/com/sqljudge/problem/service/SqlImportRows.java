package com.sqljudge.problem.service;

import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaColumnRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaTableRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Values;

final class SqlImportRows {

    private static final Pattern NEEDS_QUOTES = Pattern.compile("[\",\r\n]");

    private final String tableName;
    private final List<String> columns;
    private final List<String[]> rows = new ArrayList<>();

    private SqlImportRows(String tableName, List<String> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    static void addInsert(
            Insert insert,
            Map<String, SchemaTableRequest> tables,
            Map<String, SqlImportRows> rows,
            String prefix) {
        String tableName = SqlImportParser.unquote(insert.getTable().getName());
        Values values = insert.getSelect() instanceof Values literalValues ? literalValues : null;
        if (values == null) {
            throw SqlImportParser.invalid(
                    prefix + "INSERT chỉ được dùng VALUES với giá trị cố định");
        }
        List<String> insertColumns = insertColumnsOf(insert, tables.get(tableName), prefix);
        SqlImportRows table =
                rows.computeIfAbsent(
                        tableName,
                        name ->
                                new SqlImportRows(
                                        name, targetColumns(tables, name, insertColumns)));
        for (List<Expression> row : rowsOf(values)) {
            table.add(insertColumns, row, prefix);
        }
    }

    String tableName() {
        return tableName;
    }

    int size() {
        return rows.size();
    }

    String toCsv() {
        StringBuilder csv = new StringBuilder(String.join(",", columns)).append('\n');
        for (String[] row : rows) {
            csv.append(String.join(",", Arrays.stream(row).map(SqlImportRows::field).toList()))
                    .append('\n');
        }
        return csv.toString();
    }

    private void add(List<String> insertColumns, List<Expression> values, String prefix) {
        if (values.size() != insertColumns.size()) {
            throw SqlImportParser.invalid(prefix + "số giá trị không khớp số cột");
        }
        String[] row = new String[columns.size()];
        for (int position = 0; position < insertColumns.size(); position++) {
            int target = columns.indexOf(insertColumns.get(position));
            if (target < 0) {
                throw SqlImportParser.invalid(
                        prefix
                                + "bảng "
                                + tableName
                                + " không có cột "
                                + insertColumns.get(position));
            }
            row[target] = literalOf(values.get(position), prefix);
        }
        rows.add(row);
    }

    private static List<String> insertColumnsOf(
            Insert insert, SchemaTableRequest table, String prefix) {
        if (insert.getColumns() != null && !insert.getColumns().isEmpty()) {
            return insert.getColumns().stream()
                    .map(column -> SqlImportParser.unquote(column.getColumnName()))
                    .toList();
        }
        if (table == null) {
            throw SqlImportParser.invalid(
                    prefix
                            + "INSERT vào bảng "
                            + SqlImportParser.unquote(insert.getTable().getName())
                            + " cần liệt kê tên cột");
        }
        return table.columns().stream().map(SchemaColumnRequest::name).toList();
    }

    private static List<String> targetColumns(
            Map<String, SchemaTableRequest> tables, String tableName, List<String> insertColumns) {
        SchemaTableRequest table = tables.get(tableName);
        return table == null
                ? insertColumns
                : table.columns().stream().map(SchemaColumnRequest::name).toList();
    }

    private static List<List<Expression>> rowsOf(Values values) {
        ExpressionList<?> expressions = values.getExpressions();
        boolean isMultiRow =
                !expressions.isEmpty()
                        && expressions.stream().allMatch(ExpressionList.class::isInstance);
        if (!isMultiRow) {
            return List.of(new ArrayList<>(expressions));
        }
        return expressions.stream()
                .map(row -> (List<Expression>) new ArrayList<Expression>((ExpressionList<?>) row))
                .toList();
    }

    private static String literalOf(Expression expression, String prefix) {
        if (expression instanceof NullValue) {
            return null;
        }
        if (expression instanceof StringValue text) {
            return text.getValue().replace("''", "'");
        }
        if (expression instanceof LongValue number) {
            return number.getStringValue();
        }
        if (expression instanceof DoubleValue number) {
            return number.toString();
        }
        if (expression instanceof SignedExpression signed
                && (signed.getExpression() instanceof LongValue
                        || signed.getExpression() instanceof DoubleValue)) {
            return signed.getSign() + literalOf(signed.getExpression(), prefix);
        }
        String text = expression.toString();
        String lower = text.toLowerCase(Locale.ROOT);
        if ((lower.equals("true") || lower.equals("false"))
                && (expression instanceof Column
                        || expression.getClass().getSimpleName().equals("BooleanValue"))) {
            return lower;
        }
        throw SqlImportParser.invalid(prefix + "giá trị " + text + " không phải giá trị cố định");
    }

    private static String field(String value) {
        if (value == null) {
            return "";
        }
        return value.isEmpty() || NEEDS_QUOTES.matcher(value).find()
                ? "\"" + value.replace("\"", "\"\"") + "\""
                : value;
    }
}
