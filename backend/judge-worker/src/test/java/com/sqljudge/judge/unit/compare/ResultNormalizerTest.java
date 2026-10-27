package com.sqljudge.judge.unit.compare;

import static com.sqljudge.judge.unit.compare.Tables.columns;
import static com.sqljudge.judge.unit.compare.Tables.raw;
import static com.sqljudge.judge.unit.compare.Tables.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.judge.compare.Cell;
import com.sqljudge.judge.compare.Cell.BooleanCell;
import com.sqljudge.judge.compare.Cell.BytesCell;
import com.sqljudge.judge.compare.Cell.NonFiniteNumberCell;
import com.sqljudge.judge.compare.Cell.NullCell;
import com.sqljudge.judge.compare.Cell.NumberCell;
import com.sqljudge.judge.compare.Cell.TemporalCell;
import com.sqljudge.judge.compare.Cell.TextCell;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.ResultNormalizer;
import com.sqljudge.judge.compare.TemporalKind;
import com.sqljudge.judge.compare.UnsupportedColumnTypeException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResultNormalizerTest {

    private final ResultNormalizer normalizer = new ResultNormalizer();

    private Cell normalizeOne(Object value) {
        NormalizedTable table = normalizer.normalize(raw(columns("c"), row(value)));
        return table.rows().get(0).cells().get(0);
    }

    @Test
    @DisplayName("Giữ nguyên tên cột và thứ tự dòng")
    void normalize_keepsColumnNamesAndRowOrder() {
        NormalizedTable table =
                normalizer.normalize(raw(columns("id", "name"), row(2, "b"), row(1, "a")));

        assertThat(table.columnNames()).containsExactly("id", "name");
        assertThat(table.columnCount()).isEqualTo(2);
        assertThat(table.rows())
                .extracting(r -> r.cells().get(1))
                .containsExactly(new TextCell("b"), new TextCell("a"));
    }

    @Test
    @DisplayName("NULL thành NullCell, chuỗi 'NULL' thành TextCell")
    void normalize_nullAndNullStringAreDifferent() {
        assertThat(normalizeOne(null)).isEqualTo(new NullCell());
        assertThat(normalizeOne("NULL")).isEqualTo(new TextCell("NULL"));
    }

    @Test
    @DisplayName("Mọi kiểu số nguyên và NUMERIC thành NumberCell giữ nguyên giá trị")
    void normalize_integralAndDecimalTypes_becomeNumberCells() {
        assertThat(normalizeOne(7)).isEqualTo(new NumberCell(new BigDecimal("7")));
        assertThat(normalizeOne(7L)).isEqualTo(new NumberCell(new BigDecimal("7")));
        assertThat(normalizeOne((short) 7)).isEqualTo(new NumberCell(new BigDecimal("7")));
        assertThat(normalizeOne((byte) 7)).isEqualTo(new NumberCell(new BigDecimal("7")));
        assertThat(normalizeOne(BigInteger.valueOf(7)))
                .isEqualTo(new NumberCell(new BigDecimal("7")));
        assertThat(normalizeOne(new BigDecimal("0.00")))
                .isEqualTo(new NumberCell(new BigDecimal("0.00")));
    }

    @Test
    @DisplayName("Double và Float chuyển qua dạng thập phân ngắn nhất")
    void normalize_floatingPoint_usesShortestDecimalForm() {
        assertThat(normalizeOne(0.1d)).isEqualTo(new NumberCell(new BigDecimal("0.1")));
        assertThat(normalizeOne(0.1f)).isEqualTo(new NumberCell(new BigDecimal("0.1")));
    }

    @Test
    @DisplayName("NaN và vô cực thành NonFiniteNumberCell")
    void normalize_nonFiniteDoubles_becomeNonFiniteCells() {
        assertThat(normalizeOne(Double.NaN)).isEqualTo(new NonFiniteNumberCell("NaN"));
        assertThat(normalizeOne(Double.POSITIVE_INFINITY))
                .isEqualTo(new NonFiniteNumberCell("Infinity"));
        assertThat(normalizeOne(Float.NEGATIVE_INFINITY))
                .isEqualTo(new NonFiniteNumberCell("-Infinity"));
    }

    @Test
    @DisplayName("Chuỗi giữ nguyên khoảng trắng, hoa thường, dấu tiếng Việt và emoji")
    void normalize_text_isKeptVerbatim() {
        assertThat(normalizeOne(" Hà Nội 🚀 ")).isEqualTo(new TextCell(" Hà Nội 🚀 "));
        assertThat(normalizeOne('x')).isEqualTo(new TextCell("x"));
        UUID id = UUID.fromString("0192a3b4-0000-7000-8000-000000000001");
        assertThat(normalizeOne(id)).isEqualTo(new TextCell(id.toString()));
    }

    @Test
    @DisplayName("Boolean và byte[]")
    void normalize_booleanAndBytes() {
        assertThat(normalizeOne(true)).isEqualTo(new BooleanCell(true));
        assertThat(normalizeOne(new byte[] {(byte) 0xde, (byte) 0xad, 0x01}))
                .isEqualTo(new BytesCell("dead01"));
    }

    @Test
    @DisplayName("timestamptz ở các múi giờ khác nhau cùng chuẩn hoá về một Instant UTC")
    void normalize_zonedTimestamps_areConvertedToUtc() {
        Instant instant = Instant.parse("2026-10-11T05:30:00Z");
        OffsetDateTime hanoi = OffsetDateTime.parse("2026-10-11T12:30:00+07:00");
        ZonedDateTime tokyo = ZonedDateTime.of(2026, 10, 11, 14, 30, 0, 0, ZoneId.of("Asia/Tokyo"));
        TemporalCell expected =
                new TemporalCell(TemporalKind.TIMESTAMP_UTC, "2026-10-11T05:30:00Z");

        assertThat(normalizeOne(instant)).isEqualTo(expected);
        assertThat(normalizeOne(hanoi)).isEqualTo(expected);
        assertThat(normalizeOne(tokyo)).isEqualTo(expected);
    }

    @Test
    @DisplayName("timetz chuẩn hoá về UTC")
    void normalize_offsetTime_isConvertedToUtc() {
        OffsetTime time = OffsetTime.of(12, 30, 0, 0, ZoneOffset.ofHours(7));

        assertThat(normalizeOne(time)).isEqualTo(new TemporalCell(TemporalKind.TIME_UTC, "05:30Z"));
    }

    @Test
    @DisplayName("date, time, timestamp không múi giờ giữ nguyên dạng ISO")
    void normalize_localTemporals_keepIsoForm() {
        assertThat(normalizeOne(LocalDate.of(2026, 1, 2)))
                .isEqualTo(new TemporalCell(TemporalKind.DATE, "2026-01-02"));
        assertThat(normalizeOne(LocalTime.of(8, 5, 3)))
                .isEqualTo(new TemporalCell(TemporalKind.TIME, "08:05:03"));
        assertThat(normalizeOne(LocalDateTime.of(2026, 1, 2, 8, 5)))
                .isEqualTo(new TemporalCell(TemporalKind.TIMESTAMP, "2026-01-02T08:05"));
    }

    @Test
    @DisplayName("Kiểu chưa hỗ trợ thì báo lỗi riêng, không đoán bừa")
    void normalize_unsupportedType_throws() {
        assertThatThrownBy(() -> normalizeOne(Timestamp.from(Instant.EPOCH)))
                .isInstanceOf(UnsupportedColumnTypeException.class)
                .hasMessageContaining("java.sql.Timestamp");
        assertThatThrownBy(() -> normalizeOne(new AtomicLong(1)))
                .isInstanceOf(UnsupportedColumnTypeException.class)
                .hasMessageContaining("AtomicLong");
    }
}
