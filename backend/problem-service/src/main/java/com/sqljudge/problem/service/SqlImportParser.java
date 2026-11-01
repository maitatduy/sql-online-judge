package com.sqljudge.problem.service;

import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaTableRequest;
import com.sqljudge.problem.dto.response.SqlImportResponse;
import com.sqljudge.problem.dto.response.SqlImportResponse.TableData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.UnsupportedStatement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.insert.Insert;

public final class SqlImportParser {

    private static final String FIELD = "sql";
    private static final int MAX_ERROR_LENGTH = 200;

    private SqlImportParser() {}

    public static SqlImportResponse parse(String sql) {
        if (sql == null || sql.isBlank()) {
            throw invalid("File SQL không có câu lệnh nào");
        }
        List<Statement> statements = parseStatements(sql);
        if (statements.isEmpty()) {
            throw invalid("File SQL không có câu lệnh nào");
        }
        Map<String, SchemaTableRequest> tables = new LinkedHashMap<>();
        Map<String, SqlImportRows> rows = new LinkedHashMap<>();
        for (int index = 0; index < statements.size(); index++) {
            String prefix = "Câu lệnh thứ " + (index + 1) + ": ";
            Statement statement = statements.get(index);
            if (statement instanceof UnsupportedStatement) {
                throw invalid(
                        "Không đọc được file SQL: câu lệnh thứ "
                                + (index + 1)
                                + " sai cú pháp hoặc chưa hỗ trợ");
            }
            if (statement instanceof CreateTable createTable) {
                SchemaTableRequest table = SqlImportSchema.tableOf(createTable, prefix);
                tables.put(table.name(), table);
            } else if (statement instanceof Insert insert) {
                SqlImportRows.addInsert(insert, tables, rows, prefix);
            } else {
                throw invalid(prefix + "chỉ nhận CREATE TABLE và INSERT");
            }
        }
        return new SqlImportResponse(
                new ArrayList<>(tables.values()),
                rows.values().stream()
                        .map(table -> new TableData(table.tableName(), table.toCsv(), table.size()))
                        .toList());
    }

    static InvalidFieldException invalid(String message) {
        return new InvalidFieldException(FIELD, message);
    }

    static String unquote(String identifier) {
        String name = identifier.trim();
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            name = name.substring(lastDot + 1);
        }
        return name.replaceAll("[`\"\\[\\]]", "").toLowerCase(java.util.Locale.ROOT);
    }

    private static List<Statement> parseStatements(String sql) {
        try {
            return new ArrayList<>(CCJSqlParserUtil.parseStatements(sql));
        } catch (JSQLParserException exception) {
            String detail = String.valueOf(exception.getMessage()).lines().findFirst().orElse("");
            if (detail.length() > MAX_ERROR_LENGTH) {
                detail = detail.substring(0, MAX_ERROR_LENGTH);
            }
            throw invalid("Không đọc được file SQL: " + detail);
        }
    }
}
