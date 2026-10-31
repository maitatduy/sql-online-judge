package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.problem.entity.DatasetColumn;
import com.sqljudge.problem.entity.DatasetTable;
import com.sqljudge.problem.entity.DatasetVariant;
import com.sqljudge.problem.entity.DatasetVariantTable;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.entity.LogicalType;
import com.sqljudge.problem.service.CsvTableParser.ParsedCsv;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class DatasetReadiness {

    private static final int FIRST_DATA_LINE = 2;

    private DatasetReadiness() {}

    public static void check(DatasetVersion draft) {
        if (draft.getTables().isEmpty()) {
            throw notReady("Cần ít nhất một bảng trong schema");
        }
        if (draft.getVariants().stream().noneMatch(DatasetVariant::isPublic)) {
            throw notReady("Cần ít nhất một bộ dữ liệu công khai");
        }
        draft.getVariants().forEach(variant -> checkVariant(draft, variant));
    }

    private static void checkVariant(DatasetVersion draft, DatasetVariant variant) {
        Map<String, ParsedCsv> parsed = new HashMap<>();
        for (DatasetTable table : draft.getTables()) {
            DatasetVariantTable data =
                    variant.findTable(table.getName())
                            .orElseThrow(
                                    () ->
                                            notReady(
                                                    "Bộ dữ liệu "
                                                            + variant.getName()
                                                            + " chưa có CSV cho bảng "
                                                            + table.getName()));
            parsed.put(table.getName(), parse(variant, table, data));
        }
        for (DatasetTable table : draft.getTables()) {
            checkReferences(draft, variant, table, parsed);
        }
    }

    private static ParsedCsv parse(
            DatasetVariant variant, DatasetTable table, DatasetVariantTable data) {
        try {
            return CsvTableParser.parse(data.getCsv(), CsvTableParser.columnsOf(table));
        } catch (InvalidFieldException exception) {
            throw notReady(
                    "Bộ dữ liệu "
                            + variant.getName()
                            + ", bảng "
                            + table.getName()
                            + ": "
                            + exception.getViolation().message());
        }
    }

    private static void checkReferences(
            DatasetVersion draft,
            DatasetVariant variant,
            DatasetTable table,
            Map<String, ParsedCsv> parsed) {
        List<DatasetColumn> columns = table.getColumns();
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            DatasetColumn column = columns.get(columnIndex);
            if (column.getReferencesTable() == null) {
                continue;
            }
            Set<String> targetKeys = keysOf(draft, column, parsed);
            List<List<String>> rows = parsed.get(table.getName()).rows();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                String value = rows.get(rowIndex).get(columnIndex);
                if (value != null && !targetKeys.contains(normalized(value, column))) {
                    throw notReady(
                            "Bộ dữ liệu "
                                    + variant.getName()
                                    + ", bảng "
                                    + table.getName()
                                    + ", dòng "
                                    + (rowIndex + FIRST_DATA_LINE)
                                    + ": "
                                    + column.getName()
                                    + " = "
                                    + value
                                    + " không có trong "
                                    + column.getReferencesTable()
                                    + "."
                                    + column.getReferencesColumn());
                }
            }
        }
    }

    private static Set<String> keysOf(
            DatasetVersion draft, DatasetColumn reference, Map<String, ParsedCsv> parsed) {
        DatasetTable target = draft.findTable(reference.getReferencesTable()).orElseThrow();
        int targetIndex =
                target.getColumns().stream()
                        .map(DatasetColumn::getName)
                        .toList()
                        .indexOf(reference.getReferencesColumn());
        return parsed.get(target.getName()).rows().stream()
                .map(row -> row.get(targetIndex))
                .filter(Objects::nonNull)
                .map(value -> normalized(value, reference))
                .collect(Collectors.toSet());
    }

    private static String normalized(String value, DatasetColumn column) {
        if (column.getLogicalType() == LogicalType.INTEGER) {
            return new BigInteger(value).toString();
        }
        if (column.getLogicalType() == LogicalType.DECIMAL) {
            return new BigDecimal(value).stripTrailingZeros().toPlainString();
        }
        return value;
    }

    private static BusinessRuleException notReady(String message) {
        return new BusinessRuleException(ErrorCode.DATASET_NOT_READY, message);
    }
}
