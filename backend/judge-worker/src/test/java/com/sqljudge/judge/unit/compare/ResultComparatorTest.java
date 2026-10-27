package com.sqljudge.judge.unit.compare;

import static com.sqljudge.judge.unit.compare.Tables.columns;
import static com.sqljudge.judge.unit.compare.Tables.repeated;
import static com.sqljudge.judge.unit.compare.Tables.row;
import static com.sqljudge.judge.unit.compare.Tables.table;
import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.compare.CompareOptions;
import com.sqljudge.judge.compare.ComparisonResult;
import com.sqljudge.judge.compare.MismatchReason;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.ResultComparator;
import com.sqljudge.judge.compare.Verdict;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ResultComparatorTest {

    private static final CompareOptions UNORDERED = options(false, false);
    private static final CompareOptions ORDERED = options(true, false);
    private static final CompareOptions UNORDERED_BY_NAME = options(false, true);
    private static final CompareOptions ORDERED_BY_NAME = options(true, true);

    private final ResultComparator comparator = new ResultComparator();

    private static CompareOptions options(boolean ordered, boolean compareColumnNames) {
        return new CompareOptions(ordered, compareColumnNames, CompareOptions.DEFAULT_TOLERANCE);
    }

    private Verdict verdictOf(NormalizedTable expected, NormalizedTable actual, CompareOptions o) {
        return comparator.compare(expected, actual, o).verdict();
    }

    @Test
    @DisplayName("Hai bảng giống hệt nhau -> AC, không có diff")
    void compare_identicalTables_returnsAccepted() {
        NormalizedTable expected = table(columns("id", "name"), row(1, "An"), row(2, "Bình"));
        NormalizedTable actual = table(columns("id", "name"), row(1, "An"), row(2, "Bình"));

        ComparisonResult result = comparator.compare(expected, actual, UNORDERED);

        assertThat(result.verdict()).isEqualTo(Verdict.AC);
        assertThat(result.reason()).isEqualTo(MismatchReason.NONE);
        assertThat(result.isAccepted()).isTrue();
        assertThat(result.missingRows()).isEmpty();
        assertThat(result.extraRows()).isEmpty();
    }

    @Test
    @DisplayName("B1 · NULL so với chuỗi 'NULL' -> WA")
    void compare_nullVersusNullString_returnsWrongAnswer() {
        NormalizedTable expected = table(columns("manager"), row((Object) null));
        NormalizedTable actual = table(columns("manager"), row("NULL"));

        ComparisonResult result = comparator.compare(expected, actual, UNORDERED);

        assertThat(result.verdict()).isEqualTo(Verdict.WA);
        assertThat(result.reason()).isEqualTo(MismatchReason.ROWS);
        assertThat(result.isAccepted()).isFalse();
        assertThat(result.missingRows()).containsExactly(0);
        assertThat(result.extraRows()).containsExactly(0);
    }

    @Test
    @DisplayName("NULL so với NULL -> AC")
    void compare_nullVersusNull_returnsAccepted() {
        NormalizedTable expected = table(columns("c"), row((Object) null), row(1));
        NormalizedTable actual = table(columns("c"), row(1), row((Object) null));

        assertThat(verdictOf(expected, actual, UNORDERED)).isEqualTo(Verdict.AC);
    }

    @Test
    @DisplayName("Hai dòng NULL trùng nhau được đếm đủ, thiếu một dòng -> WA")
    void compare_duplicateNullRows_countedAsMultiset() {
        NormalizedTable expected = table(columns("c"), row((Object) null), row((Object) null));
        NormalizedTable same = table(columns("c"), row((Object) null), row((Object) null));
        NormalizedTable single = table(columns("c"), row((Object) null));

        assertThat(verdictOf(expected, same, UNORDERED)).isEqualTo(Verdict.AC);
        assertThat(verdictOf(expected, single, UNORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("B2 · 0.1 + 0.2 (double) so với 0.3 -> AC")
    void compare_floatingPointRoundingError_isWithinTolerance() {
        NormalizedTable expected = table(columns("total"), row(new BigDecimal("0.3")));
        NormalizedTable actual = table(columns("total"), row(0.1d + 0.2d));

        assertThat(verdictOf(expected, actual, UNORDERED)).isEqualTo(Verdict.AC);
        assertThat(verdictOf(expected, actual, ORDERED)).isEqualTo(Verdict.AC);
    }

    @Test
    @DisplayName("B3 · Số lệch 1e-3 -> WA")
    void compare_numberOffByOneThousandth_returnsWrongAnswer() {
        NormalizedTable expected = table(columns("avg"), row(new BigDecimal("2.500")));
        NormalizedTable actual = table(columns("avg"), row(new BigDecimal("2.501")));

        assertThat(verdictOf(expected, actual, UNORDERED)).isEqualTo(Verdict.WA);
        assertThat(verdictOf(expected, actual, ORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("NUMERIC 0 bằng 0.00, số nguyên bằng số thực cùng giá trị")
    void compare_numbersByValue_ignoresScale() {
        NormalizedTable expected = table(columns("a", "b"), row(new BigDecimal("0"), 5));
        NormalizedTable actual = table(columns("a", "b"), row(new BigDecimal("0.00"), 5.0d));

        assertThat(verdictOf(expected, actual, ORDERED)).isEqualTo(Verdict.AC);
    }

    @Test
    @DisplayName("Sai số cấu hình được: nới tới 1e-2 thì lệch 1e-3 thành AC")
    void compare_customTolerance_isApplied() {
        NormalizedTable expected = table(columns("avg"), row(new BigDecimal("2.500")));
        NormalizedTable actual = table(columns("avg"), row(new BigDecimal("2.501")));
        CompareOptions loose = new CompareOptions(false, false, new BigDecimal("0.01"));

        assertThat(verdictOf(expected, actual, loose)).isEqualTo(Verdict.AC);
    }

    @Test
    @DisplayName("Lệch đúng bằng sai số vẫn tính là bằng, lệch hơn một chút thì khác")
    void compare_differenceAtToleranceBoundary() {
        NormalizedTable expected = table(columns("x"), row(new BigDecimal("1.000000")));
        NormalizedTable atBoundary = table(columns("x"), row(new BigDecimal("1.000001")));
        NormalizedTable beyond = table(columns("x"), row(new BigDecimal("1.0000011")));

        assertThat(verdictOf(expected, atBoundary, UNORDERED)).isEqualTo(Verdict.AC);
        assertThat(verdictOf(expected, beyond, UNORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("Số lớn dùng sai số tương đối: tổng double 1e12 lệch 0,5 vẫn AC, lệch 2e6 thì WA")
    void compare_largeNumbers_useRelativeTolerance() {
        NormalizedTable expected = table(columns("total"), row(new BigDecimal("1000000000000")));
        NormalizedTable rounding = table(columns("total"), row(1000000000000.5d));
        NormalizedTable wrong = table(columns("total"), row(1000002000000d));

        assertThat(verdictOf(expected, rounding, UNORDERED)).isEqualTo(Verdict.AC);
        assertThat(verdictOf(expected, wrong, UNORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("Số nhỏ hơn 1 vẫn dùng sai số tuyệt đối 1e-6")
    void compare_smallNumbers_useAbsoluteTolerance() {
        NormalizedTable expected = table(columns("rate"), row(new BigDecimal("0.0000001")));
        NormalizedTable close = table(columns("rate"), row(new BigDecimal("0.0000009")));
        NormalizedTable far = table(columns("rate"), row(new BigDecimal("0.0000020")));

        assertThat(verdictOf(expected, close, UNORDERED)).isEqualTo(Verdict.AC);
        assertThat(verdictOf(expected, far, UNORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("Số 1 khác chuỗi '1'")
    void compare_numberVersusNumericString_returnsWrongAnswer() {
        NormalizedTable expected = table(columns("x"), row(1));
        NormalizedTable actual = table(columns("x"), row("1"));

        assertThat(verdictOf(expected, actual, ORDERED)).isEqualTo(Verdict.WA);
        assertThat(verdictOf(expected, actual, UNORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("NaN chỉ bằng NaN, không bằng vô cực")
    void compare_nonFiniteNumbers_matchOnlyThemselves() {
        NormalizedTable expected = table(columns("x"), row(Double.NaN), row(1.0d));
        NormalizedTable same = table(columns("x"), row(1.0d), row(Double.NaN));
        NormalizedTable other = table(columns("x"), row(Double.POSITIVE_INFINITY), row(1.0d));

        assertThat(verdictOf(expected, same, UNORDERED)).isEqualTo(Verdict.AC);
        assertThat(verdictOf(expected, other, UNORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("Boolean và byte[] so theo giá trị")
    void compare_booleanAndBytes_byValue() {
        NormalizedTable expected = table(columns("b", "bytes"), row(true, new byte[] {1, 2}));
        NormalizedTable same = table(columns("b", "bytes"), row(true, new byte[] {1, 2}));
        NormalizedTable flipped = table(columns("b", "bytes"), row(false, new byte[] {1, 2}));
        NormalizedTable otherBytes = table(columns("b", "bytes"), row(true, new byte[] {1, 3}));

        assertThat(verdictOf(expected, same, UNORDERED)).isEqualTo(Verdict.AC);
        assertThat(verdictOf(expected, flipped, UNORDERED)).isEqualTo(Verdict.WA);
        assertThat(verdictOf(expected, otherBytes, UNORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("B4 · Thiếu một dòng trùng -> WA, dòng thiếu nằm trong missingRows")
    void compare_missingDuplicateRow_returnsWrongAnswer() {
        NormalizedTable expected = table(columns("dept"), row("IT"), row("IT"), row("HR"));
        NormalizedTable actual = table(columns("dept"), row("HR"), row("IT"));

        ComparisonResult result = comparator.compare(expected, actual, UNORDERED);

        assertThat(result.verdict()).isEqualTo(Verdict.WA);
        assertThat(result.reason()).isEqualTo(MismatchReason.ROWS);
        assertThat(result.missingRows()).containsExactly(1);
        assertThat(result.extraRows()).isEmpty();
    }

    @Test
    @DisplayName("Thừa một dòng trùng -> WA, dòng thừa nằm trong extraRows")
    void compare_extraDuplicateRow_returnsWrongAnswer() {
        NormalizedTable expected = table(columns("dept"), row("HR"), row("IT"));
        NormalizedTable actual = table(columns("dept"), row("IT"), row("HR"), row("HR"));

        ComparisonResult result = comparator.compare(expected, actual, UNORDERED);

        assertThat(result.verdict()).isEqualTo(Verdict.WA);
        assertThat(result.missingRows()).isEmpty();
        assertThat(result.extraRows()).containsExactly(2);
    }

    @Test
    @DisplayName("B5 · Đúng dữ liệu, sai thứ tự, đề yêu cầu thứ tự -> OM, không có diff")
    void compare_sameRowsWrongOrder_whenOrdered_returnsOrderMismatch() {
        NormalizedTable expected =
                table(
                        columns("dept", "salary"),
                        row("Kinh doanh", 32000000),
                        row("Kỹ thuật", 48000000));
        NormalizedTable actual =
                table(
                        columns("dept", "salary"),
                        row("Kỹ thuật", 48000000),
                        row("Kinh doanh", 32000000));

        ComparisonResult result = comparator.compare(expected, actual, ORDERED);

        assertThat(result.verdict()).isEqualTo(Verdict.OM);
        assertThat(result.reason()).isEqualTo(MismatchReason.ORDER);
        assertThat(result.firstOrderMismatchRow()).isZero();
        assertThat(result.missingRows()).isEmpty();
        assertThat(result.extraRows()).isEmpty();
    }

    @Test
    @DisplayName("OM chỉ ra dòng đầu tiên lệch thứ tự")
    void compare_orderMismatch_reportsFirstMismatchedRow() {
        NormalizedTable expected = table(columns("id"), row(1), row(2), row(3));
        NormalizedTable actual = table(columns("id"), row(1), row(3), row(2));

        ComparisonResult result = comparator.compare(expected, actual, ORDERED);

        assertThat(result.verdict()).isEqualTo(Verdict.OM);
        assertThat(result.firstOrderMismatchRow()).isEqualTo(1);
    }

    @Test
    @DisplayName("Chỉ số dòng thiếu/thừa theo thứ tự gốc của bảng, không theo thứ tự sắp xếp")
    void compare_diffIndices_followOriginalOrder() {
        NormalizedTable expected = table(columns("v"), row("c"), row("a"), row("b"));
        NormalizedTable actual = table(columns("v"), row("z"), row("a"), row("y"));

        ComparisonResult result = comparator.compare(expected, actual, UNORDERED);

        assertThat(result.missingRows()).containsExactly(0, 2);
        assertThat(result.extraRows()).containsExactly(0, 2);
        assertThat(result.firstOrderMismatchRow()).isNull();
    }

    @Test
    @DisplayName("B6 · Sai thứ tự, đề không yêu cầu thứ tự -> AC")
    void compare_sameRowsWrongOrder_whenUnordered_returnsAccepted() {
        NormalizedTable expected = table(columns("id"), row(1), row(2), row(3));
        NormalizedTable actual = table(columns("id"), row(3), row(1), row(2));

        assertThat(verdictOf(expected, actual, UNORDERED)).isEqualTo(Verdict.AC);
    }

    @Test
    @DisplayName("Đề yêu cầu thứ tự, đúng thứ tự -> AC")
    void compare_sameRowsSameOrder_whenOrdered_returnsAccepted() {
        NormalizedTable expected = table(columns("id"), row(1), row(2));
        NormalizedTable actual = table(columns("id"), row(1), row(2));

        assertThat(verdictOf(expected, actual, ORDERED)).isEqualTo(Verdict.AC);
    }

    @Test
    @DisplayName("Đề yêu cầu thứ tự, sai cả dữ liệu -> WA kèm diff, không phải OM")
    void compare_differentRows_whenOrdered_returnsWrongAnswerWithDiff() {
        NormalizedTable expected = table(columns("id"), row(1), row(2));
        NormalizedTable actual = table(columns("id"), row(2), row(3));

        ComparisonResult result = comparator.compare(expected, actual, ORDERED);

        assertThat(result.verdict()).isEqualTo(Verdict.WA);
        assertThat(result.reason()).isEqualTo(MismatchReason.ROWS);
        assertThat(result.missingRows()).containsExactly(0);
        assertThat(result.extraRows()).containsExactly(1);
    }

    @Test
    @DisplayName("Đề yêu cầu thứ tự, thiếu dòng cuối -> WA")
    void compare_differentRowCount_whenOrdered_returnsWrongAnswer() {
        NormalizedTable expected = table(columns("id"), row(1), row(2));
        NormalizedTable actual = table(columns("id"), row(1));

        ComparisonResult result = comparator.compare(expected, actual, ORDERED);

        assertThat(result.verdict()).isEqualTo(Verdict.WA);
        assertThat(result.missingRows()).containsExactly(1);
    }

    @Nested
    @DisplayName("B7 · Bảng rỗng")
    class EmptyTables {

        @Test
        @DisplayName("Hai bảng rỗng -> AC")
        void compare_bothEmpty_returnsAccepted() {
            NormalizedTable expected = table(columns("id"));
            NormalizedTable actual = table(columns("id"));

            assertThat(verdictOf(expected, actual, UNORDERED)).isEqualTo(Verdict.AC);
            assertThat(verdictOf(expected, actual, ORDERED)).isEqualTo(Verdict.AC);
        }

        @Test
        @DisplayName("Mong đợi rỗng nhưng thực tế có dòng -> WA")
        void compare_expectedEmptyActualHasRows_returnsWrongAnswer() {
            NormalizedTable expected = table(columns("id"));
            NormalizedTable actual = table(columns("id"), row(1));

            ComparisonResult result = comparator.compare(expected, actual, UNORDERED);

            assertThat(result.verdict()).isEqualTo(Verdict.WA);
            assertThat(result.extraRows()).containsExactly(0);
        }

        @Test
        @DisplayName("Mong đợi có dòng nhưng thực tế rỗng -> WA")
        void compare_actualEmpty_returnsWrongAnswer() {
            NormalizedTable expected = table(columns("id"), row(1));
            NormalizedTable actual = table(columns("id"));

            ComparisonResult result = comparator.compare(expected, actual, ORDERED);

            assertThat(result.verdict()).isEqualTo(Verdict.WA);
            assertThat(result.missingRows()).containsExactly(0);
        }
    }

    @Test
    @DisplayName("B8 · Khác số cột -> WA, lý do COLUMN_COUNT, không kèm diff")
    void compare_differentColumnCount_returnsWrongAnswer() {
        NormalizedTable expected = table(columns("id", "name"), row(1, "An"));
        NormalizedTable actual = table(columns("id"), row(1));

        ComparisonResult result = comparator.compare(expected, actual, ORDERED);

        assertThat(result.verdict()).isEqualTo(Verdict.WA);
        assertThat(result.reason()).isEqualTo(MismatchReason.COLUMN_COUNT);
        assertThat(result.missingRows()).isEmpty();
        assertThat(result.extraRows()).isEmpty();
    }

    @Nested
    @DisplayName("B9 · Khác tên cột")
    class ColumnNames {

        private final NormalizedTable expected =
                table(columns("department", "max_salary"), row("IT", 1));
        private final NormalizedTable renamed = table(columns("dept", "max"), row("IT", 1));

        @Test
        @DisplayName("compare_column_names tắt -> AC")
        void compare_differentNames_whenNotCompared_returnsAccepted() {
            assertThat(verdictOf(expected, renamed, UNORDERED)).isEqualTo(Verdict.AC);
        }

        @Test
        @DisplayName("compare_column_names bật -> WA, lý do COLUMN_NAMES")
        void compare_differentNames_whenCompared_returnsWrongAnswer() {
            ComparisonResult result = comparator.compare(expected, renamed, UNORDERED_BY_NAME);

            assertThat(result.verdict()).isEqualTo(Verdict.WA);
            assertThat(result.reason()).isEqualTo(MismatchReason.COLUMN_NAMES);
        }

        @Test
        @DisplayName("compare_column_names bật, tên trùng khớp -> AC")
        void compare_sameNames_whenCompared_returnsAccepted() {
            NormalizedTable same = table(columns("department", "max_salary"), row("IT", 1));

            assertThat(verdictOf(expected, same, ORDERED_BY_NAME)).isEqualTo(Verdict.AC);
        }

        @Test
        @DisplayName("compare_column_names bật, chỉ khác hoa thường -> AC")
        void compare_namesDifferInCase_whenCompared_returnsAccepted() {
            NormalizedTable upper = table(columns("Department", "MAX_SALARY"), row("IT", 1));

            assertThat(verdictOf(expected, upper, UNORDERED_BY_NAME)).isEqualTo(Verdict.AC);
        }
    }

    @Test
    @DisplayName("B10 · Khoảng trắng cuối chuỗi -> WA")
    void compare_trailingWhitespace_returnsWrongAnswer() {
        NormalizedTable expected = table(columns("name"), row("An"));
        NormalizedTable actual = table(columns("name"), row("An "));

        assertThat(verdictOf(expected, actual, UNORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("B10 · Khác hoa thường -> WA")
    void compare_differentCase_returnsWrongAnswer() {
        NormalizedTable expected = table(columns("name"), row("An"));
        NormalizedTable actual = table(columns("name"), row("an"));

        assertThat(verdictOf(expected, actual, UNORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("B11 · Tiếng Việt có dấu và emoji so đúng từng ký tự")
    void compare_vietnameseAndEmoji_comparedExactly() {
        NormalizedTable expected = table(columns("city"), row("Hà Nội 🏙️"), row("Đà Nẵng"));
        NormalizedTable same = table(columns("city"), row("Đà Nẵng"), row("Hà Nội 🏙️"));
        NormalizedTable noTone = table(columns("city"), row("Ha Noi 🏙️"), row("Đà Nẵng"));
        NormalizedTable otherEmoji = table(columns("city"), row("Hà Nội 🌆"), row("Đà Nẵng"));

        assertThat(verdictOf(expected, same, UNORDERED)).isEqualTo(Verdict.AC);
        assertThat(verdictOf(expected, noTone, UNORDERED)).isEqualTo(Verdict.WA);
        assertThat(verdictOf(expected, otherEmoji, UNORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("B12 · timestamptz khác múi giờ cùng thời điểm -> AC sau khi chuẩn hoá UTC")
    void compare_sameInstantDifferentZones_returnsAccepted() {
        NormalizedTable expected =
                table(columns("at"), row(OffsetDateTime.parse("2026-10-11T12:30:00+07:00")));
        NormalizedTable sameInstant =
                table(columns("at"), row(OffsetDateTime.parse("2026-10-11T05:30:00Z")));
        NormalizedTable sameWallClock =
                table(columns("at"), row(OffsetDateTime.parse("2026-10-11T12:30:00Z")));

        assertThat(verdictOf(expected, sameInstant, ORDERED)).isEqualTo(Verdict.AC);
        assertThat(verdictOf(expected, sameWallClock, ORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("Ngày khác nhau -> WA; date khác timestamp cùng nửa đêm")
    void compare_temporalValues_comparedByKindAndValue() {
        NormalizedTable expected = table(columns("d"), row(LocalDate.of(2026, 1, 1)));
        NormalizedTable otherDay = table(columns("d"), row(LocalDate.of(2026, 1, 2)));
        NormalizedTable midnight = table(columns("d"), row(LocalDateTime.of(2026, 1, 1, 0, 0)));

        assertThat(verdictOf(expected, otherDay, UNORDERED)).isEqualTo(Verdict.WA);
        assertThat(verdictOf(expected, midnight, UNORDERED)).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("Bảng trộn nhiều loại giá trị vẫn sắp xếp ổn định và so đúng")
    void compare_mixedCellKinds_unordered_returnsAccepted() {
        NormalizedTable expected =
                table(
                        columns("v"),
                        row(true),
                        row((Object) null),
                        row(new byte[] {1}),
                        row(Double.NaN),
                        row(LocalDate.of(2026, 1, 1)),
                        row("text"),
                        row(1),
                        row(false));
        NormalizedTable actual =
                table(
                        columns("v"),
                        row(1),
                        row(false),
                        row("text"),
                        row(LocalDate.of(2026, 1, 1)),
                        row(Double.NaN),
                        row(new byte[] {1}),
                        row((Object) null),
                        row(true));

        assertThat(verdictOf(expected, actual, UNORDERED)).isEqualTo(Verdict.AC);
    }

    @Test
    @DisplayName("Dòng nhiều cột: khác ở cột sau vẫn bị phát hiện, diff đúng dòng")
    void compare_multiColumnRowsDifferInLaterColumn_returnsWrongAnswer() {
        NormalizedTable expected = table(columns("a", "b"), row(1, "x"), row(1, "y"));
        NormalizedTable actual = table(columns("a", "b"), row(1, "y"), row(1, "z"));

        ComparisonResult result = comparator.compare(expected, actual, UNORDERED);

        assertThat(result.verdict()).isEqualTo(Verdict.WA);
        assertThat(result.missingRows()).containsExactly(0);
        assertThat(result.extraRows()).containsExactly(1);
    }

    @Test
    @DisplayName("Giới hạn đã biết: cột đầu chênh dưới sai số, cột sau đảo cặp -> WA")
    void compare_nearlyEqualLeadingNumbersWithSwappedLabels_knownLimitation() {
        NormalizedTable expected =
                table(
                        columns("a", "b"),
                        row(new BigDecimal("1.0"), "b"),
                        row(new BigDecimal("1.0000001"), "a"));
        NormalizedTable actual =
                table(
                        columns("a", "b"),
                        row(new BigDecimal("1.0000001"), "b"),
                        row(new BigDecimal("1.0"), "a"));

        assertThat(verdictOf(expected, actual, UNORDERED)).isEqualTo(Verdict.WA);
    }

    @Nested
    @DisplayName("B13 · Biên giới hạn 10.000 dòng")
    class RowLimit {

        private static final int MAX_ROWS = 10_000;

        @Test
        @DisplayName("Đủ 10.000 dòng giống nhau -> AC, cả có và không thứ tự")
        void compare_exactlyMaxRows_returnsAccepted() {
            NormalizedTable expected = repeated(columns("id", "label"), MAX_ROWS);
            NormalizedTable actual = repeated(columns("id", "label"), MAX_ROWS);

            assertThat(verdictOf(expected, actual, UNORDERED)).isEqualTo(Verdict.AC);
            assertThat(verdictOf(expected, actual, ORDERED)).isEqualTo(Verdict.AC);
        }

        @Test
        @DisplayName(
                "Thực tế bị cắt ở 10.000 dòng trong khi mong đợi 9.999 -> WA, thừa đúng 1 dòng")
        void compare_truncatedActualLongerThanExpected_returnsWrongAnswer() {
            NormalizedTable expected = repeated(columns("id", "label"), MAX_ROWS - 1);
            NormalizedTable actual = repeated(columns("id", "label"), MAX_ROWS);

            ComparisonResult result = comparator.compare(expected, actual, UNORDERED);

            assertThat(result.verdict()).isEqualTo(Verdict.WA);
            assertThat(result.missingRows()).isEmpty();
            assertThat(result.extraRows()).containsExactly(MAX_ROWS - 1);
        }
    }
}
