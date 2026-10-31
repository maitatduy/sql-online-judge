package com.sqljudge.problem.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.problem.entity.LogicalType;
import com.sqljudge.problem.service.CsvTableParser;
import com.sqljudge.problem.service.CsvTableParser.CsvColumn;
import com.sqljudge.problem.service.CsvTableParser.ParsedCsv;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CsvTableParserTest {

    private static final List<CsvColumn> EMPLOYEES =
            List.of(
                    new CsvColumn("id", LogicalType.INTEGER, true, false),
                    new CsvColumn("full_name", LogicalType.TEXT, false, false),
                    new CsvColumn("salary", LogicalType.DECIMAL, false, true));

    @Test
    @DisplayName("Đọc CSV hợp lệ: ô trống là NULL, ô có nháy kép giữ được dấu phẩy và xuống dòng")
    void parse_validCsv_returnsRows() {
        ParsedCsv parsed =
                CsvTableParser.parse(
                        "id,full_name,salary\r\n"
                                + "1,\"Nguyễn, Văn An\",48000000.5\r\n"
                                + "2,\"Dòng một\nDòng hai\",\r\n"
                                + "3,\"Nói \"\"xin chào\"\"\",-1\n",
                        EMPLOYEES);

        assertThat(parsed.rowCount()).isEqualTo(3);
        assertThat(parsed.rows())
                .containsExactly(
                        List.of("1", "Nguyễn, Văn An", "48000000.5"),
                        Arrays.asList("2", "Dòng một\nDòng hai", null),
                        List.of("3", "Nói \"xin chào\"", "-1"));
    }

    @Test
    @DisplayName("Ô văn bản có nháy kép rỗng là chuỗi rỗng, không phải NULL")
    void parse_quotedEmptyText_isEmptyString() {
        ParsedCsv parsed = CsvTableParser.parse("id,full_name,salary\n1,\"\",\n", EMPLOYEES);

        assertThat(parsed.rows()).containsExactly(Arrays.asList("1", "", null));
    }

    @Test
    @DisplayName("CSV chỉ có dòng tiêu đề là bảng rỗng")
    void parse_headerOnly_returnsNoRows() {
        assertThat(CsvTableParser.parse("id,full_name,salary", EMPLOYEES).rowCount()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "CSV trống|''|CSV trống, cần ít nhất dòng tiêu đề",
                "Sai tiêu đề|'id,name,salary\n'|Dòng tiêu đề phải là: id,full_name,salary",
                "Thiếu giá trị|'id,full_name,salary\n1,An\n'|Dòng 2 có 2 giá trị, cần 3",
                "Nháy kép chưa đóng|'id,full_name,salary\n1,\"An,5\n'|Dòng 2 có dấu nháy kép chưa"
                        + " đóng",
                "Cột bắt buộc để trống|'id,full_name,salary\n1,,5\n'|Dòng 2, cột full_name không"
                        + " được để trống",
                "Sai kiểu số nguyên|'id,full_name,salary\nmot,An,5\n'|Dòng 2, cột id phải là số"
                        + " nguyên",
                "Sai kiểu số thập phân|'id,full_name,salary\n1,An,5k\n'|Dòng 2, cột salary phải là"
                        + " số thập phân",
                "Trùng khoá chính|'id,full_name,salary\n1,An,\n2,Bình,\n1,Châu,\n'|Dòng 4 trùng"
                        + " khoá chính với dòng 2"
            })
    void parse_invalidCsv_reportsFirstProblem(String scenario, String csv, String message) {
        assertThatThrownBy(() -> CsvTableParser.parse(csv, EMPLOYEES))
                .isInstanceOf(InvalidFieldException.class)
                .extracting("violation.field", "violation.message")
                .containsExactly("csv", message);
    }

    @ParameterizedTest(name = "{0} {1} hợp lệ là {2}")
    @CsvSource({
        "DATE, 2026-10-31, true",
        "DATE, 2026-02-30, false",
        "DATE, 31/10/2026, false",
        "TIMESTAMP, 2026-10-31 08:30:00, true",
        "TIMESTAMP, 2026-10-31T08:30:00, true",
        "TIMESTAMP, 2026-10-31 25:00:00, false",
        "BOOLEAN, true, true",
        "BOOLEAN, FALSE, true",
        "BOOLEAN, 1, true",
        "BOOLEAN, yes, false",
        "INTEGER, -42, true",
        "INTEGER, 1.5, false",
        "DECIMAL, .5, false",
        "DECIMAL, -0.25, true"
    })
    void parse_typedValue_isValidatedByLogicalType(String type, String value, boolean valid) {
        List<CsvColumn> columns =
                List.of(new CsvColumn("value", LogicalType.valueOf(type), false, false));
        String csv = "value\n" + value + "\n";

        if (valid) {
            assertThat(CsvTableParser.parse(csv, columns).rowCount()).isEqualTo(1);
        } else {
            assertThatThrownBy(() -> CsvTableParser.parse(csv, columns))
                    .isInstanceOf(InvalidFieldException.class);
        }
    }

    @Test
    @DisplayName("Khoá chính ghép từ nhiều cột chỉ trùng khi trùng mọi cột")
    void parse_compositeKey_comparesAllKeyColumns() {
        List<CsvColumn> enrollments =
                List.of(
                        new CsvColumn("student_id", LogicalType.INTEGER, true, false),
                        new CsvColumn("course_id", LogicalType.INTEGER, true, false));

        assertThat(
                        CsvTableParser.parse("student_id,course_id\n1,1\n1,2\n2,1\n", enrollments)
                                .rowCount())
                .isEqualTo(3);
        assertThatThrownBy(
                        () -> CsvTableParser.parse("student_id,course_id\n1,1\n1,1\n", enrollments))
                .isInstanceOf(InvalidFieldException.class);
    }
}
