package com.sqljudge.judge.integration.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.SqliteSandbox;
import com.sqljudge.judge.sandbox.SqliteSandboxConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SqliteSandboxIntegrationTest {

    private static final String NUMBERS_TO_1000 =
            "WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 1000) ";

    @TempDir Path workDirectory;
    @TempDir Path outsideDirectory;

    private SqliteSandbox sandbox;

    @BeforeEach
    void createSandbox() {
        sandbox = new SqliteSandbox(new SqliteSandboxConfig(workDirectory));
    }

    @AfterEach
    void assertNothingLeftBehind() throws IOException {
        try (Stream<Path> files = Files.list(workDirectory)) {
            assertThat(files).isEmpty();
        }
        try (Stream<Path> files = Files.list(outsideDirectory)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    @DisplayName("Chạy SELECT trên dataset đã nạp, ngày giờ là chuỗi ISO, boolean là 0 hoặc 1")
    void run_select_returnsTable() {
        ExecutionOutcome outcome =
                run(
                        "SELECT e.full_name, e.salary, d.name AS department, e.hired_at,"
                                + " e.is_active, e.updated_at"
                                + " FROM employees e LEFT JOIN departments d"
                                + " ON d.id = e.department_id ORDER BY e.id");

        assertThat(outcome).isInstanceOf(Succeeded.class);
        ResultTable table = ((Succeeded) outcome).table();
        assertThat(table.columnNames())
                .containsExactly(
                        "full_name", "salary", "department", "hired_at", "is_active", "updated_at");
        List<Object> first = table.rows().get(0);
        List<Object> second = table.rows().get(1);
        assertThat(first.get(0)).isEqualTo("Hoàng Quốc Bảo");
        assertThat(((Number) first.get(1)).doubleValue()).isEqualTo(48000000.5);
        assertThat(first.subList(2, 6))
                .containsExactly("Kỹ thuật", "2024-03-01", 1, "2026-01-05 09:30:00");
        assertThat(((Number) second.get(1)).longValue()).isEqualTo(30000000L);
        assertThat(second.subList(2, 6)).containsExactly(null, "2025-07-15", 0, null);
    }

    @Test
    @DisplayName("Khoá chính kiểu TEXT và khoá ngoại vẫn tạo được bảng")
    void run_textPrimaryKey_isSupported() {
        ExecutionOutcome outcome =
                sandbox.run(
                        new QueryRun(
                                SandboxFixtures.textKeyed(),
                                "SELECT c.id, r.name FROM cities c JOIN regions r"
                                        + " ON r.code = c.region_code",
                                SandboxFixtures.FAST_LIMITS));

        assertThat(((Succeeded) outcome).table().rows()).containsExactly(List.of(1, "Hà Nội"));
    }

    @Test
    @DisplayName("Lỗi cú pháp trả Failed với thông báo gọn, không lộ đường dẫn file")
    void run_syntaxError_returnsSanitizedFailure() {
        ExecutionOutcome outcome = run("SELECT nam FROM employees");

        assertThat(((Failed) outcome).message().text()).isEqualTo("no such column: nam");
    }

    @Test
    @DisplayName("Kết quả nhiều hơn giới hạn dòng thì dừng đọc và báo lỗi")
    void run_tooManyRows_returnsFailure() {
        ExecutionOutcome outcome = run(NUMBERS_TO_1000 + "SELECT a.i FROM n a, n b");

        assertThat(((Failed) outcome).message().text()).isEqualTo("Kết quả có hơn 100 dòng");
    }

    @Test
    @DisplayName("Kết quả vượt dung lượng cho phép thì dừng đọc và báo lỗi")
    void run_resultTooLarge_returnsFailure() {
        ExecutionOutcome outcome =
                run(NUMBERS_TO_1000 + "SELECT hex(zeroblob(10000)) FROM n WHERE i <= 10");

        assertThat(((Failed) outcome).message().text())
                .isEqualTo("Kết quả vượt quá dung lượng cho phép");
    }

    @Nested
    @DisplayName("Bộ truy vấn tấn công")
    class Attacks {

        @ParameterizedTest
        @DisplayName("Lệnh ghi dữ liệu, đổi cấu trúc, gắn file hay nạp extension đều bị chặn")
        @ValueSource(
                strings = {
                    "DROP TABLE employees",
                    "DELETE FROM employees",
                    "UPDATE employees SET salary = 0",
                    "INSERT INTO departments VALUES (9, 'X')",
                    "CREATE TABLE stolen (id INTEGER)",
                    "CREATE TEMP TABLE stolen AS SELECT * FROM employees",
                    "SELECT load_extension('/usr/lib/libsqlitefunctions.so')",
                    "SELECT randomblob(2000000000)",
                    "PRAGMA writable_schema = 1"
                })
        void run_destructiveStatement_isBlocked(String sql) {
            assertThat(run(sql)).isInstanceOf(Failed.class);
        }

        @Test
        @DisplayName("ATTACH và VACUUM INTO không tạo được file nào bên ngoài")
        void run_attachOrVacuumInto_writesNoFile() {
            String target = outsideDirectory.resolve("leak.db").toString().replace("'", "''");

            assertThat(run("ATTACH DATABASE '" + target + "' AS leak")).isInstanceOf(Failed.class);
            assertThat(run("VACUUM INTO '" + target + "'")).isInstanceOf(Failed.class);
        }

        @Test
        @DisplayName("Nhiều câu lệnh nối bằng dấu chấm phẩy không làm hỏng dữ liệu")
        void run_chainedStatements_areHarmless() {
            ExecutionOutcome outcome = run("SELECT COUNT(*) FROM employees; DROP TABLE employees");

            if (outcome instanceof Succeeded succeeded) {
                assertThat(succeeded.table().rows()).containsExactly(List.of(2));
            } else {
                assertThat(outcome).isInstanceOf(Failed.class);
            }
        }

        @ParameterizedTest
        @DisplayName("Câu chạy quá lâu bị dừng theo giới hạn thời gian, không treo worker")
        @ValueSource(
                strings = {
                    "WITH RECURSIVE r(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM r)"
                            + " SELECT COUNT(*) FROM r",
                    NUMBERS_TO_1000 + "SELECT COUNT(*) FROM n a, n b, n c, n d"
                })
        void run_slowQuery_timesOut(String sql) {
            long startedAt = System.nanoTime();

            ExecutionOutcome outcome = run(sql);

            assertThat(outcome).isInstanceOf(TimedOut.class);
            assertThat(Duration.ofNanos(System.nanoTime() - startedAt))
                    .isLessThan(Duration.ofSeconds(10));
        }
    }

    private ExecutionOutcome run(String sql) {
        return sandbox.run(
                new QueryRun(SandboxFixtures.company(), sql, SandboxFixtures.FAST_LIMITS));
    }
}
