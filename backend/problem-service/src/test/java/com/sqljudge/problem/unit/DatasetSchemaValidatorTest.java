package com.sqljudge.problem.unit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.ColumnReferenceRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaColumnRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaTableRequest;
import com.sqljudge.problem.entity.LogicalType;
import com.sqljudge.problem.service.DatasetSchemaValidator;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DatasetSchemaValidatorTest {

    @Test
    @DisplayName("Schema phòng ban và nhân viên có khoá ngoại và tự tham chiếu là hợp lệ")
    void validate_companySchema_passes() {
        assertThatCode(
                        () ->
                                DatasetSchemaValidator.validate(
                                        schema(
                                                departments(),
                                                table(
                                                        "employees",
                                                        primaryKey("id"),
                                                        reference(
                                                                "department_id",
                                                                "departments",
                                                                "id"),
                                                        reference(
                                                                "manager_id", "employees", "id")))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Tên bảng trùng từ khoá SQL thì báo lỗi ở tên bảng")
    void validate_reservedTableName_rejectsTableName() {
        assertRejected(
                () -> DatasetSchemaValidator.validate(schema(table("order", primaryKey("id")))),
                "tables[0].name",
                "Tên này trùng từ khoá SQL");
    }

    @Test
    @DisplayName("Tên cột trùng từ khoá SQL thì báo lỗi ở tên cột")
    void validate_reservedColumnName_rejectsColumnName() {
        assertRejected(
                () ->
                        DatasetSchemaValidator.validate(
                                schema(table("orders", primaryKey("id"), text("user")))),
                "tables[0].columns[1].name",
                "Tên này trùng từ khoá SQL");
    }

    @Test
    @DisplayName("Hai bảng trùng tên thì báo lỗi ở bảng thứ hai")
    void validate_duplicateTableName_rejectsSecondTable() {
        assertRejected(
                () -> DatasetSchemaValidator.validate(schema(departments(), departments())),
                "tables[1].name",
                "Tên bảng bị trùng");
    }

    @Test
    @DisplayName("Hai cột trùng tên trong một bảng thì báo lỗi ở cột thứ hai")
    void validate_duplicateColumnName_rejectsSecondColumn() {
        assertRejected(
                () ->
                        DatasetSchemaValidator.validate(
                                schema(table("departments", primaryKey("id"), text("id")))),
                "tables[0].columns[1].name",
                "Tên cột bị trùng trong bảng");
    }

    @Test
    @DisplayName("Cột khoá chính cho phép rỗng thì bị chặn")
    void validate_nullablePrimaryKey_isRejected() {
        SchemaColumnRequest nullableKey =
                new SchemaColumnRequest("id", LogicalType.INTEGER, true, true, null);

        assertRejected(
                () -> DatasetSchemaValidator.validate(schema(table("departments", nullableKey))),
                "tables[0].columns[0].isNullable",
                "Cột khoá chính không được để trống");
    }

    @Test
    @DisplayName("Khoá ngoại trỏ tới bảng không có thì báo lỗi ở khoá ngoại")
    void validate_referenceToUnknownTable_isRejected() {
        assertRejected(
                () ->
                        DatasetSchemaValidator.validate(
                                schema(
                                        table(
                                                "employees",
                                                primaryKey("id"),
                                                reference("department_id", "departments", "id")))),
                "tables[0].columns[1].references",
                "Khoá ngoại phải trỏ tới cột khoá chính của một bảng trong schema");
    }

    @Test
    @DisplayName("Khoá ngoại trỏ tới cột không phải khoá chính thì bị chặn")
    void validate_referenceToNonKeyColumn_isRejected() {
        assertRejected(
                () ->
                        DatasetSchemaValidator.validate(
                                schema(
                                        departments(),
                                        table(
                                                "employees",
                                                primaryKey("id"),
                                                reference(
                                                        "department_id", "departments", "name")))),
                "tables[1].columns[1].references",
                "Khoá ngoại phải trỏ tới cột khoá chính của một bảng trong schema");
    }

    @Test
    @DisplayName("Khoá ngoại khác kiểu với cột được trỏ tới thì bị chặn")
    void validate_referenceWithDifferentType_isRejected() {
        SchemaColumnRequest textReference =
                new SchemaColumnRequest(
                        "department_id",
                        LogicalType.TEXT,
                        false,
                        true,
                        new ColumnReferenceRequest("departments", "id"));

        assertRejected(
                () ->
                        DatasetSchemaValidator.validate(
                                schema(
                                        departments(),
                                        table("employees", primaryKey("id"), textReference))),
                "tables[1].columns[1].references",
                "Khoá ngoại phải cùng kiểu với cột được trỏ tới");
    }

    private static void assertRejected(ThrowingCallable call, String field, String message) {
        assertThatThrownBy(call)
                .isInstanceOf(InvalidFieldException.class)
                .extracting("violation.field", "violation.message")
                .containsExactly(field, message);
    }

    private static UpdateDatasetSchemaRequest schema(SchemaTableRequest... tables) {
        return new UpdateDatasetSchemaRequest(List.of(tables));
    }

    private static SchemaTableRequest departments() {
        return table("departments", primaryKey("id"), text("name"));
    }

    private static SchemaTableRequest table(String name, SchemaColumnRequest... columns) {
        return new SchemaTableRequest(name, List.of(columns));
    }

    private static SchemaColumnRequest primaryKey(String name) {
        return new SchemaColumnRequest(name, LogicalType.INTEGER, true, false, null);
    }

    private static SchemaColumnRequest text(String name) {
        return new SchemaColumnRequest(name, LogicalType.TEXT, false, false, null);
    }

    private static SchemaColumnRequest reference(String name, String table, String column) {
        return new SchemaColumnRequest(
                name, LogicalType.INTEGER, false, true, new ColumnReferenceRequest(table, column));
    }
}
