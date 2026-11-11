package com.sqljudge.judge.unit.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.compare.Cell;
import com.sqljudge.judge.compare.Cell.BytesCell;
import com.sqljudge.judge.compare.Cell.NullCell;
import com.sqljudge.judge.compare.Cell.NumberCell;
import com.sqljudge.judge.compare.Cell.TextCell;
import com.sqljudge.judge.compare.CompareOptions;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.ResultComparator;
import com.sqljudge.judge.compare.Row;
import com.sqljudge.judge.job.ExpectedTable.CellKind;
import com.sqljudge.judge.job.ExpectedTable.WireCell;
import com.sqljudge.judge.job.ResultDiff;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResultDiffTest {

    private static final CompareOptions UNORDERED =
            new CompareOptions(false, false, CompareOptions.DEFAULT_TOLERANCE);

    @Test
    @DisplayName("Chỉ giữ tối đa 20 dòng thừa, số dòng vẫn là số thật")
    void of_manyExtraRows_keepsAtMostTwentyRows() {
        NormalizedTable expected = table(List.of(row(new NumberCell(BigDecimal.ONE))));
        NormalizedTable actual =
                table(
                        IntStream.rangeClosed(1, 30)
                                .mapToObj(i -> row(new NumberCell(BigDecimal.valueOf(i))))
                                .toList());

        ResultDiff diff = diffOf(expected, actual);

        assertThat(diff.actualRowCount()).isEqualTo(30);
        assertThat(diff.extraRows()).hasSize(ResultDiff.MAX_ROWS);
        assertThat(diff.extraRows().get(0)).containsExactly(new WireCell(CellKind.NUMBER, "2"));
    }

    @Test
    @DisplayName("Giá trị chữ và byte quá dài bị cắt bớt, NULL giữ nguyên")
    void of_longValues_areShortened() {
        String longText = "x".repeat(ResultDiff.MAX_CELL_CHARS + 50);
        NormalizedTable expected = table(List.of(row(new TextCell("a"), new NullCell())));
        NormalizedTable actual =
                table(List.of(row(new TextCell(longText), new BytesCell("ab".repeat(400)))));

        ResultDiff diff = diffOf(expected, actual);

        List<WireCell> extra = diff.extraRows().get(0);
        assertThat(extra.get(0).value()).hasSize(ResultDiff.MAX_CELL_CHARS + 1).endsWith("…");
        assertThat(extra.get(1).value()).hasSize(ResultDiff.MAX_CELL_CHARS + 1);
        assertThat(diff.missingRows().get(0).get(1)).isEqualTo(new WireCell(CellKind.NULL, null));
    }

    @Test
    @DisplayName("Khác số cột thì chỉ có tên cột hai bên, không có dòng")
    void of_columnCountDiffers_hasColumnsOnly() {
        NormalizedTable expected =
                new NormalizedTable(List.of("id"), List.of(row(new NumberCell(BigDecimal.ONE))));
        NormalizedTable actual =
                new NormalizedTable(
                        List.of("id", "name"),
                        List.of(row(new NumberCell(BigDecimal.ONE), new TextCell("A"))));

        ResultDiff diff = diffOf(expected, actual);

        assertThat(diff.expectedColumns()).containsExactly("id");
        assertThat(diff.actualColumns()).containsExactly("id", "name");
        assertThat(diff.missingRows()).isEmpty();
        assertThat(diff.extraRows()).isEmpty();
    }

    @Test
    @DisplayName("Chỉ giữ tối đa 20 cột cho tên cột và từng dòng")
    void of_manyColumns_keepsAtMostTwentyColumns() {
        Cell[] cells =
                IntStream.range(0, 30)
                        .mapToObj(i -> new NumberCell(BigDecimal.valueOf(i)))
                        .toArray(Cell[]::new);
        NormalizedTable expected = table(List.of(row(cells)));
        cells[0] = new NumberCell(BigDecimal.TEN.negate());
        NormalizedTable actual = table(List.of(row(cells)));

        ResultDiff diff = diffOf(expected, actual);

        assertThat(diff.expectedColumns()).hasSize(ResultDiff.MAX_COLUMNS);
        assertThat(diff.missingRows().get(0)).hasSize(ResultDiff.MAX_COLUMNS);
        assertThat(diff.extraRows().get(0)).hasSize(ResultDiff.MAX_COLUMNS);
    }

    private static ResultDiff diffOf(NormalizedTable expected, NormalizedTable actual) {
        return ResultDiff.of(
                expected, actual, new ResultComparator().compare(expected, actual, UNORDERED));
    }

    private static NormalizedTable table(List<Row> rows) {
        List<String> columns =
                IntStream.range(0, rows.get(0).cells().size()).mapToObj(i -> "c" + i).toList();
        return new NormalizedTable(columns, rows);
    }

    private static Row row(Cell... cells) {
        return new Row(List.of(cells));
    }
}
