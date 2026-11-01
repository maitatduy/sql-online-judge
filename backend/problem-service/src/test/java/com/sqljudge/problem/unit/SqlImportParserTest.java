package com.sqljudge.problem.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.ColumnReferenceRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaColumnRequest;
import com.sqljudge.problem.dto.request.UpdateDatasetSchemaRequest.SchemaTableRequest;
import com.sqljudge.problem.dto.response.SqlImportResponse;
import com.sqljudge.problem.dto.response.SqlImportResponse.TableData;
import com.sqljudge.problem.entity.LogicalType;
import com.sqljudge.problem.service.SqlImportParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SqlImportParserTest {

    private static final String COMPANY_SQL =
            """
            CREATE TABLE departments (id SERIAL PRIMARY KEY, name VARCHAR(100) NOT NULL);
            CREATE TABLE employees (
              id INT NOT NULL,
              full_name TEXT NOT NULL,
              salary NUMERIC(12, 2),
              hired_on DATE,
              active BOOLEAN,
              department_id INT REFERENCES departments (id),
              PRIMARY KEY (id)
            );
            INSERT INTO departments (id, name) VALUES (1, 'Kỹ thuật'), (2, 'Kinh doanh, bán hàng');
            INSERT INTO employees VALUES
              (1, 'O''Brien', 48000000.50, '2026-01-15', TRUE, 1),
              (2, '', -1, NULL, false, NULL);
            """;

    @Test
    @DisplayName("Đọc CREATE TABLE thành schema: kiểu, khoá chính, NOT NULL, khoá ngoại")
    void parse_createTable_buildsSchema() {
        SqlImportResponse result = SqlImportParser.parse(COMPANY_SQL);

        assertThat(result.tables())
                .extracting(SchemaTableRequest::name)
                .containsExactly("departments", "employees");
        assertThat(result.tables().get(0).columns())
                .extracting(
                        SchemaColumnRequest::name,
                        SchemaColumnRequest::type,
                        SchemaColumnRequest::isPrimaryKey,
                        SchemaColumnRequest::isNullable)
                .containsExactly(
                        tuple("id", LogicalType.INTEGER, true, false),
                        tuple("name", LogicalType.TEXT, false, false));
        assertThat(result.tables().get(1).columns())
                .extracting(
                        SchemaColumnRequest::name,
                        SchemaColumnRequest::type,
                        SchemaColumnRequest::isPrimaryKey,
                        SchemaColumnRequest::isNullable,
                        SchemaColumnRequest::references)
                .containsExactly(
                        tuple("id", LogicalType.INTEGER, true, false, null),
                        tuple("full_name", LogicalType.TEXT, false, false, null),
                        tuple("salary", LogicalType.DECIMAL, false, true, null),
                        tuple("hired_on", LogicalType.DATE, false, true, null),
                        tuple("active", LogicalType.BOOLEAN, false, true, null),
                        tuple(
                                "department_id",
                                LogicalType.INTEGER,
                                false,
                                true,
                                new ColumnReferenceRequest("departments", "id")));
    }

    @Test
    @DisplayName("Đọc INSERT thành CSV, giữ dấu phẩy, nháy đơn, chuỗi rỗng và NULL")
    void parse_insert_buildsCsvPerTable() {
        SqlImportResponse result = SqlImportParser.parse(COMPANY_SQL);

        assertThat(result.data())
                .extracting(TableData::tableName, TableData::csv, TableData::rowCount)
                .containsExactly(
                        tuple(
                                "departments",
                                "id,name\n1,Kỹ thuật\n2,\"Kinh doanh, bán hàng\"\n",
                                2),
                        tuple(
                                "employees",
                                "id,full_name,salary,hired_on,active,department_id\n"
                                        + "1,O'Brien,48000000.50,2026-01-15,true,1\n"
                                        + "2,\"\",-1,,false,\n",
                                2));
    }

    @Test
    @DisplayName("Hiểu cú pháp MySQL: tên trong dấu `, khoá ngoại khai báo ở cuối bảng")
    void parse_mysqlDump_readsBackticksAndTableLevelKeys() {
        SqlImportResponse result =
                SqlImportParser.parse(
                        "CREATE TABLE `Orders` (`id` BIGINT AUTO_INCREMENT, `customer_id` INT NOT"
                                + " NULL, `ordered_at` DATETIME, PRIMARY KEY (`id`), FOREIGN KEY"
                                + " (`customer_id`) REFERENCES `customers` (`id`)) ENGINE=InnoDB;");

        assertThat(result.tables().get(0).name()).isEqualTo("orders");
        assertThat(result.tables().get(0).columns())
                .extracting(
                        SchemaColumnRequest::name,
                        SchemaColumnRequest::type,
                        SchemaColumnRequest::isPrimaryKey,
                        SchemaColumnRequest::references)
                .containsExactly(
                        tuple("id", LogicalType.INTEGER, true, null),
                        tuple(
                                "customer_id",
                                LogicalType.INTEGER,
                                false,
                                new ColumnReferenceRequest("customers", "id")),
                        tuple("ordered_at", LogicalType.TIMESTAMP, false, null));
    }

    @Test
    @DisplayName(
            "Chỉ có INSERT kèm tên cột thì trả dữ liệu, không có schema; nhiều INSERT được gộp")
    void parse_insertsOnly_returnsDataWithoutSchema() {
        SqlImportResponse result =
                SqlImportParser.parse(
                        "INSERT INTO public.classes (id, name) VALUES (1, 'A1');"
                                + " INSERT INTO public.classes (id, name) VALUES (2, 'A2');");

        assertThat(result.tables()).isEmpty();
        assertThat(result.data())
                .extracting(TableData::tableName, TableData::csv, TableData::rowCount)
                .containsExactly(tuple("classes", "id,name\n1,A1\n2,A2\n", 2));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "File trống|'  '|File SQL không có câu lệnh nào",
                "Tạo bảng từ truy vấn|'CREATE TABLE a AS SELECT * FROM b;'|Câu lệnh thứ 1:"
                        + " CREATE TABLE phải liệt kê cột, không dùng AS SELECT",
                "Lệnh bị cấm|'CREATE TABLE a (id INT); DROP TABLE a;'|Câu lệnh thứ 2: chỉ nhận"
                        + " CREATE TABLE và INSERT",
                "INSERT SELECT|'INSERT INTO a (id) SELECT id FROM b;'|Câu lệnh thứ 1: INSERT chỉ"
                        + " được dùng VALUES với giá trị cố định",
                "Gọi hàm|'INSERT INTO a (id, at) VALUES (1, NOW());'|Câu lệnh thứ 1: giá trị NOW()"
                        + " không phải giá trị cố định",
                "Kiểu chưa hỗ trợ|'CREATE TABLE a (id INT, data JSONB);'|Câu lệnh thứ 1: kiểu JSONB"
                        + " của cột data chưa hỗ trợ",
                "INSERT thiếu tên cột|'INSERT INTO a VALUES (1);'|Câu lệnh thứ 1: INSERT vào bảng a"
                        + " cần liệt kê tên cột",
                "Lệch số giá trị|'INSERT INTO a (id, name) VALUES (1);'|Câu lệnh thứ 1: số giá trị"
                        + " không khớp số cột"
            })
    void parse_invalidSql_reportsStatement(String scenario, String sql, String message) {
        assertThatThrownBy(() -> SqlImportParser.parse(sql))
                .isInstanceOf(InvalidFieldException.class)
                .extracting("violation.field", "violation.message")
                .containsExactly("sql", message);
    }

    @Test
    @DisplayName("Sai cú pháp thì báo không đọc được file SQL")
    void parse_syntaxError_reportsUnreadableFile() {
        assertThatThrownBy(() -> SqlImportParser.parse("CREATE TABLE (;"))
                .isInstanceOf(InvalidFieldException.class)
                .extracting("violation.message")
                .asString()
                .startsWith("Không đọc được file SQL");
    }
}
