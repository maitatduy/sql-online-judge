package com.sqljudge.problem.service;

import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.problem.entity.DatasetTable;
import com.sqljudge.problem.entity.LogicalType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class CsvTableParser {

    public static final int MAX_ROWS = 50_000;
    private static final String FIELD = "csv";
    private static final int DATE_LENGTH = 10;
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Set<String> BOOLEANS = Set.of("true", "false", "1", "0");
    private static final Map<LogicalType, String> TYPE_NAMES =
            Map.of(
                    LogicalType.INTEGER, "số nguyên",
                    LogicalType.DECIMAL, "số thập phân",
                    LogicalType.DATE, "ngày dạng YYYY-MM-DD",
                    LogicalType.TIMESTAMP, "thời điểm dạng YYYY-MM-DD HH:MM:SS",
                    LogicalType.BOOLEAN, "true hoặc false");

    private CsvTableParser() {}

    public static ParsedCsv parse(String csv, List<CsvColumn> columns) {
        List<List<Field>> records = readRecords(csv);
        if (records.isEmpty()) {
            throw invalid("CSV trống, cần ít nhất dòng tiêu đề");
        }
        requireHeader(records.get(0), columns);
        if (records.size() - 1 > MAX_ROWS) {
            throw invalid("Tối đa 50.000 dòng dữ liệu");
        }
        List<List<String>> rows = new ArrayList<>();
        Map<List<String>, Integer> keyLines = new HashMap<>();
        for (int index = 1; index < records.size(); index++) {
            int line = index + 1;
            List<String> row = toRow(records.get(index), columns, line);
            requireUniqueKey(row, columns, line, keyLines);
            rows.add(row);
        }
        return new ParsedCsv(rows.size(), rows);
    }

    public static List<CsvColumn> columnsOf(DatasetTable table) {
        return table.getColumns().stream()
                .map(
                        column ->
                                new CsvColumn(
                                        column.getName(),
                                        column.getLogicalType(),
                                        column.isPrimaryKey(),
                                        column.isNullable()))
                .toList();
    }

    private static void requireHeader(List<Field> header, List<CsvColumn> columns) {
        List<String> expected = columns.stream().map(CsvColumn::name).toList();
        List<String> actual = header.stream().map(Field::value).toList();
        if (!actual.equals(expected)) {
            throw invalid("Dòng tiêu đề phải là: " + String.join(",", expected));
        }
    }

    private static List<String> toRow(List<Field> record, List<CsvColumn> columns, int line) {
        if (record.size() != columns.size()) {
            throw invalid(
                    "Dòng " + line + " có " + record.size() + " giá trị, cần " + columns.size());
        }
        List<String> row = new ArrayList<>(columns.size());
        for (int index = 0; index < columns.size(); index++) {
            Field field = record.get(index);
            CsvColumn column = columns.get(index);
            String value = field.value().isEmpty() && !field.quoted() ? null : field.value();
            requireValid(value, column, line);
            row.add(value);
        }
        return row;
    }

    private static void requireValid(String value, CsvColumn column, int line) {
        if (value == null) {
            if (!column.nullable()) {
                throw invalid("Dòng " + line + ", cột " + column.name() + " không được để trống");
            }
            return;
        }
        if (!matchesType(value, column.type())) {
            throw invalid(
                    "Dòng "
                            + line
                            + ", cột "
                            + column.name()
                            + " phải là "
                            + TYPE_NAMES.get(column.type()));
        }
    }

    private static boolean matchesType(String value, LogicalType type) {
        return switch (type) {
            case TEXT -> true;
            case INTEGER -> INTEGER.matcher(value).matches() && fitsInLong(value);
            case DECIMAL -> DECIMAL.matcher(value).matches();
            case BOOLEAN -> BOOLEANS.contains(value.toLowerCase(Locale.ROOT));
            case DATE -> parses(() -> LocalDate.parse(value));
            case TIMESTAMP -> parses(() -> LocalDateTime.parse(isoTimestamp(value)));
        };
    }

    private static String isoTimestamp(String value) {
        return value.length() > DATE_LENGTH && value.charAt(DATE_LENGTH) == ' '
                ? value.substring(0, DATE_LENGTH) + "T" + value.substring(DATE_LENGTH + 1)
                : value;
    }

    private static boolean fitsInLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean parses(Runnable parse) {
        try {
            parse.run();
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private static void requireUniqueKey(
            List<String> row, List<CsvColumn> columns, int line, Map<List<String>, Integer> seen) {
        List<String> key = new ArrayList<>();
        for (int index = 0; index < columns.size(); index++) {
            if (columns.get(index).primaryKey()) {
                key.add(row.get(index));
            }
        }
        if (key.isEmpty()) {
            return;
        }
        Integer firstLine = seen.putIfAbsent(key, line);
        if (firstLine != null) {
            throw invalid("Dòng " + line + " trùng khoá chính với dòng " + firstLine);
        }
    }

    private static List<List<Field>> readRecords(String csv) {
        RecordReader reader = new RecordReader();
        for (int index = 0; index < csv.length(); index++) {
            index = reader.accept(csv, index);
        }
        return reader.finish();
    }

    private static InvalidFieldException invalid(String message) {
        return new InvalidFieldException(FIELD, message);
    }

    public record CsvColumn(String name, LogicalType type, boolean primaryKey, boolean nullable) {}

    public record ParsedCsv(int rowCount, List<List<String>> rows) {}

    private record Field(String value, boolean quoted) {}

    private static final class RecordReader {

        private final List<List<Field>> records = new ArrayList<>();
        private List<Field> fields = new ArrayList<>();
        private StringBuilder value = new StringBuilder();
        private boolean quoted;
        private boolean inQuotes;

        int accept(String csv, int index) {
            char character = csv.charAt(index);
            boolean hasNext = index + 1 < csv.length();
            if (inQuotes) {
                if (character != '"') {
                    value.append(character);
                } else if (hasNext && csv.charAt(index + 1) == '"') {
                    value.append('"');
                    return index + 1;
                } else {
                    inQuotes = false;
                }
                return index;
            }
            if (character == '"' && value.isEmpty() && !quoted) {
                inQuotes = true;
                quoted = true;
            } else if (character == ',') {
                endField();
            } else if (character == '\r' || character == '\n') {
                endRecord();
                return character == '\r' && hasNext && csv.charAt(index + 1) == '\n'
                        ? index + 1
                        : index;
            } else {
                value.append(character);
            }
            return index;
        }

        List<List<Field>> finish() {
            if (inQuotes) {
                throw invalid("Dòng " + (records.size() + 1) + " có dấu nháy kép chưa đóng");
            }
            if (!value.isEmpty() || quoted || !fields.isEmpty()) {
                endRecord();
            }
            return records;
        }

        private void endField() {
            fields.add(new Field(value.toString(), quoted));
            value = new StringBuilder();
            quoted = false;
        }

        private void endRecord() {
            endField();
            records.add(fields);
            fields = new ArrayList<>();
        }
    }
}
