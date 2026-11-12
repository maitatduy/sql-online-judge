package com.sqljudge.judge.unit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.judge.client.ExpectedTables;
import com.sqljudge.judge.compare.Cell.BytesCell;
import com.sqljudge.judge.compare.Cell.NonFiniteNumberCell;
import com.sqljudge.judge.compare.Cell.NullCell;
import com.sqljudge.judge.compare.Cell.NumberCell;
import com.sqljudge.judge.compare.Cell.TemporalCell;
import com.sqljudge.judge.compare.Cell.TextCell;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.TemporalKind;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExpectedTablesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Ô dạng {kind, value} do kiểm tra đáp án lưu được đọc lại đúng loại")
    void decode_wireCells_mapsEveryKind() throws Exception {
        JsonNode rows =
                objectMapper.readTree(
                        """
                        [[{"kind":"NUMBER","value":"32000000"},{"kind":"TEXT","value":"Kỹ thuật"},
                          {"kind":"NULL","value":null},{"kind":"DATE","value":"2024-03-01"},
                          {"kind":"TIMESTAMP_UTC","value":"2024-03-01T08:00:00Z"},
                          {"kind":"NON_FINITE_NUMBER","value":"NaN"},{"kind":"BYTES","value":"0aff"}]]
                        """);

        NormalizedTable table =
                ExpectedTables.decode(List.of("a", "b", "c", "d", "e", "f", "g"), rows);

        assertThat(table.columnNames()).containsExactly("a", "b", "c", "d", "e", "f", "g");
        assertThat(table.rows().get(0).cells())
                .containsExactly(
                        new NumberCell(new BigDecimal("32000000")),
                        new TextCell("Kỹ thuật"),
                        new NullCell(),
                        new TemporalCell(TemporalKind.DATE, "2024-03-01"),
                        new TemporalCell(TemporalKind.TIMESTAMP_UTC, "2024-03-01T08:00:00Z"),
                        new NonFiniteNumberCell("NaN"),
                        new BytesCell("0aff"));
    }

    @Test
    @DisplayName("Ô không có loại thì bị từ chối, không đoán chuỗi chữ số là số")
    void decode_cellWithoutKind_isRejected() throws Exception {
        JsonNode rows = objectMapper.readTree("[[\"007\", \"Kỹ thuật\"]]");

        assertThatThrownBy(() -> ExpectedTables.decode(List.of("code", "name"), rows))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Bảng mong đợi rỗng vẫn giữ tên cột")
    void decode_emptyRows_keepsColumns() throws Exception {
        NormalizedTable table = ExpectedTables.decode(List.of("name"), objectMapper.readTree("[]"));

        assertThat(table.columnNames()).containsExactly("name");
        assertThat(table.rows()).isEmpty();
    }
}
