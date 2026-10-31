package com.sqljudge.problem.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.entity.DatasetColumn;
import com.sqljudge.problem.entity.DatasetTable;
import com.sqljudge.problem.entity.DatasetVariant;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.entity.LogicalType;
import com.sqljudge.problem.service.DatasetReadiness;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DatasetReadinessTest {

    private static final String DEPARTMENTS_CSV = "id,name\n1,Kỹ thuật\n2,Kinh doanh\n";
    private static final String EMPLOYEES_CSV = "id,department_id\n1,1\n2,\n3,2\n";

    @Test
    @DisplayName("Bản nháp đủ bảng, có bộ dữ liệu công khai, đủ CSV, khoá ngoại khớp thì chốt được")
    void check_completeDraft_passes() {
        DatasetVersion draft = companyDraft();
        variant(draft, "Mẫu", true, DEPARTMENTS_CSV, EMPLOYEES_CSV);
        variant(draft, "Có NULL", false, DEPARTMENTS_CSV, "id,department_id\n1,\n");

        assertThatCode(() -> DatasetReadiness.check(draft)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Chưa có bảng nào thì không chốt được")
    void check_noTables_isRejected() {
        DatasetVersion draft = new Dataset().addVersion(1);
        draft.addVariant("Mẫu", true);

        assertNotReady(draft, "Cần ít nhất một bảng trong schema");
    }

    @Test
    @DisplayName("Không có bộ dữ liệu công khai thì không chốt được")
    void check_noPublicVariant_isRejected() {
        DatasetVersion draft = companyDraft();
        variant(draft, "Ẩn", false, DEPARTMENTS_CSV, EMPLOYEES_CSV);

        assertNotReady(draft, "Cần ít nhất một bộ dữ liệu công khai");
    }

    @Test
    @DisplayName("Bộ dữ liệu thiếu CSV của một bảng thì báo đúng bộ và bảng")
    void check_missingCsv_namesVariantAndTable() {
        DatasetVersion draft = companyDraft();
        draft.addVariant("Mẫu", true).addTable("departments", DEPARTMENTS_CSV, 2);

        assertNotReady(draft, "Bộ dữ liệu Mẫu chưa có CSV cho bảng employees");
    }

    @Test
    @DisplayName("CSV không còn khớp schema sau khi sửa schema thì báo lỗi của bộ đọc CSV")
    void check_csvOutdatedBySchema_reportsParserProblem() {
        DatasetVersion draft = companyDraft();
        variant(draft, "Mẫu", true, "id,title\n1,A\n", EMPLOYEES_CSV);

        assertNotReady(draft, "Bộ dữ liệu Mẫu, bảng departments: Dòng tiêu đề phải là: id,name");
    }

    @Test
    @DisplayName("Giá trị khoá ngoại không có trong bảng được trỏ tới thì báo dòng và giá trị")
    void check_danglingForeignKey_isRejected() {
        DatasetVersion draft = companyDraft();
        variant(draft, "Mẫu", true, DEPARTMENTS_CSV, "id,department_id\n1,1\n2,9\n");

        assertNotReady(
                draft,
                "Bộ dữ liệu Mẫu, bảng employees, dòng 3: department_id = 9 không có trong"
                        + " departments.id");
    }

    @Test
    @DisplayName("Khoá ngoại so theo giá trị số nên 01 khớp với 1")
    void check_numericForeignKey_comparesNumerically() {
        DatasetVersion draft = companyDraft();
        variant(draft, "Mẫu", true, DEPARTMENTS_CSV, "id,department_id\n1,01\n");

        assertThatCode(() -> DatasetReadiness.check(draft)).doesNotThrowAnyException();
    }

    private static void assertNotReady(DatasetVersion draft, String message) {
        assertThatThrownBy(() -> DatasetReadiness.check(draft))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(message)
                .satisfies(
                        error ->
                                assertThat(((BusinessRuleException) error).getCode().name())
                                        .isEqualTo("DATASET_NOT_READY"));
    }

    private static DatasetVersion companyDraft() {
        DatasetVersion draft = new Dataset().addVersion(1);
        DatasetTable departments = draft.addTable("departments");
        departments.addColumn("id", LogicalType.INTEGER).setPrimaryKey(true);
        departments.addColumn("name", LogicalType.TEXT);
        DatasetTable employees = draft.addTable("employees");
        employees.addColumn("id", LogicalType.INTEGER).setPrimaryKey(true);
        DatasetColumn departmentId = employees.addColumn("department_id", LogicalType.INTEGER);
        departmentId.setNullable(true);
        departmentId.setReferencesTable("departments");
        departmentId.setReferencesColumn("id");
        return draft;
    }

    private static void variant(
            DatasetVersion draft,
            String name,
            boolean isPublic,
            String departmentsCsv,
            String employeesCsv) {
        DatasetVariant variant = draft.addVariant(name, isPublic);
        variant.addTable("departments", departmentsCsv, 0);
        variant.addTable("employees", employeesCsv, 0);
    }
}
