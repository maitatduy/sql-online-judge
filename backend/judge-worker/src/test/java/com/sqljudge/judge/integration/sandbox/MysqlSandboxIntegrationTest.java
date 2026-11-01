package com.sqljudge.judge.integration.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.sandbox.MysqlSandbox;
import com.sqljudge.judge.sandbox.MysqlSandboxConfig;
import com.sqljudge.judge.sandbox.QueryRun;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

class MysqlSandboxIntegrationTest {

    private static final String ADMIN_USER = "judge_admin";
    private static final String ADMIN_PASSWORD = "admin-secret";
    private static final Path INIT_SCRIPT =
            Path.of("..", "..", "infra", "sandbox", "mysql", "init", "01-create-judge-admin.sh");
    private static final long MYSQL_MEMORY_LIMIT_BYTES = 768L * 1024 * 1024;
    private static final String NUMBERS_TO_1000 =
            "WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 1000) ";

    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withEnv("SANDBOX_MYSQL_ADMIN_USER", ADMIN_USER)
                    .withEnv("SANDBOX_MYSQL_ADMIN_PASSWORD", ADMIN_PASSWORD)
                    .withCommand(
                            "--local-infile=0",
                            "--secure-file-priv=NULL",
                            "--skip-log-bin",
                            "--performance-schema=OFF",
                            "--innodb-buffer-pool-size=128M")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(INIT_SCRIPT),
                            "/docker-entrypoint-initdb.d/01-create-judge-admin.sh")
                    .withCreateContainerCmdModifier(
                            command ->
                                    command.getHostConfig().withMemory(MYSQL_MEMORY_LIMIT_BYTES));

    private static MysqlSandbox sandbox;

    @BeforeAll
    static void startSandbox() {
        MYSQL.start();
        String serverUrl =
                "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getFirstMappedPort() + "/";
        sandbox = new MysqlSandbox(new MysqlSandboxConfig(serverUrl, ADMIN_USER, ADMIN_PASSWORD));
    }

    @AfterEach
    void assertNothingLeftBehind() throws SQLException {
        assertThat(
                        countAsRoot(
                                "SELECT COUNT(*) FROM information_schema.schemata"
                                        + " WHERE schema_name LIKE 'sb\\_%'"))
                .isZero();
        assertThat(countAsRoot("SELECT COUNT(*) FROM mysql.user WHERE user LIKE 'sb\\_%'"))
                .isZero();
    }

    @Test
    @DisplayName("Chạy SELECT trên dataset đã nạp, trả đúng tên cột và giá trị theo kiểu")
    void run_select_returnsTypedTable() {
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
        List<List<Object>> rows = table.rows();
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get(0)).isEqualTo("Hoàng Quốc Bảo");
        assertThat((BigDecimal) rows.get(0).get(1)).isEqualByComparingTo("48000000.50");
        assertThat(rows.get(0).subList(2, 6))
                .containsExactly(
                        "Kỹ thuật",
                        LocalDate.of(2024, 3, 1),
                        true,
                        LocalDateTime.of(2026, 1, 5, 9, 30));
        assertThat(rows.get(1).subList(2, 6))
                .containsExactly(null, LocalDate.of(2025, 7, 15), false, null);
    }

    @Test
    @DisplayName("So sánh chuỗi phân biệt hoa thường và dấu như PostgreSQL")
    void run_textComparison_isCaseAndAccentSensitive() {
        ExecutionOutcome outcome =
                run("SELECT COUNT(*) AS total FROM departments WHERE name = 'ky thuat'");

        assertThat(((Succeeded) outcome).table().rows()).containsExactly(List.of(0L));
    }

    @Test
    @DisplayName("Khoá chính kiểu TEXT vẫn tạo được bảng")
    void run_textPrimaryKey_isSupported() {
        ExecutionOutcome outcome =
                sandbox.run(
                        new QueryRun(
                                SandboxFixtures.textKeyed(),
                                "SELECT code FROM regions",
                                SandboxFixtures.FAST_LIMITS));

        assertThat(((Succeeded) outcome).table().rows()).containsExactly(List.of("HN"));
    }

    @Test
    @DisplayName("Lỗi cú pháp trả Failed, thông báo không lộ tên database tạm hay host")
    void run_syntaxError_returnsSanitizedFailure() {
        ExecutionOutcome denied = run("SELECT * FROM mysql.user");
        ExecutionOutcome unknown = run("SELECT nam FROM employees");

        assertThat(((Failed) unknown).message().text()).contains("nam").doesNotContain("sb_");
        assertThat(((Failed) denied).message().text()).doesNotContain("sb_").doesNotContain("@");
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
                run(NUMBERS_TO_1000 + "SELECT REPEAT('x', 20000) FROM n WHERE i <= 10");

        assertThat(((Failed) outcome).message().text())
                .isEqualTo("Kết quả vượt quá dung lượng cho phép");
    }

    @Nested
    @DisplayName("Bộ truy vấn tấn công")
    class Attacks {

        @ParameterizedTest
        @DisplayName("Lệnh phá dữ liệu, cấu trúc, quyền và đọc ghi file đều bị DB chặn")
        @ValueSource(
                strings = {
                    "DROP TABLE employees",
                    "TRUNCATE TABLE employees",
                    "DELETE FROM employees",
                    "UPDATE employees SET salary = 0",
                    "CREATE TABLE stolen (id INT)",
                    "CREATE TEMPORARY TABLE stolen AS SELECT * FROM employees",
                    "DROP DATABASE mysql",
                    "CREATE USER hacker IDENTIFIED BY 'x'",
                    "GRANT ALL ON *.* TO CURRENT_USER()",
                    "SELECT * FROM employees INTO OUTFILE '/tmp/leak.csv'",
                    "LOAD DATA INFILE '/etc/passwd' INTO TABLE employees",
                    "SET @x = 1",
                    "SELECT 1; DROP TABLE employees"
                })
        void run_destructiveStatement_isBlocked(String sql) {
            assertThat(run(sql)).isInstanceOf(Failed.class);
        }

        @Test
        @DisplayName("LOAD_FILE không đọc được file hệ thống")
        void run_loadFile_readsNothing() {
            ExecutionOutcome outcome = run("SELECT LOAD_FILE('/etc/passwd') AS content");

            assertThat(((Succeeded) outcome).table().rows())
                    .containsExactly(Arrays.asList((Object) null));
        }

        @Test
        @DisplayName("Không đọc được database của lần chạy khác, chỉ thấy bảng của mình")
        void run_otherDatabases_areNotReadable() throws SQLException {
            executeAsRoot("CREATE DATABASE other_run");
            executeAsRoot("CREATE TABLE other_run.answers (secret TEXT)");
            try {
                assertThat(run("SELECT * FROM other_run.answers")).isInstanceOf(Failed.class);
                ExecutionOutcome visible =
                        run(
                                "SELECT table_name FROM information_schema.tables"
                                        + " WHERE table_schema NOT IN ('information_schema',"
                                        + " 'performance_schema', 'mysql', 'sys')"
                                        + " ORDER BY table_name");
                assertThat(((Succeeded) visible).table().rows())
                        .containsExactly(List.of("departments"), List.of("employees"));
            } finally {
                executeAsRoot("DROP DATABASE other_run");
            }
        }

        @ParameterizedTest
        @DisplayName("Câu chạy quá lâu bị dừng theo giới hạn thời gian, không treo worker")
        @ValueSource(
                strings = {
                    "SELECT SLEEP(60)",
                    "SELECT /*+ MAX_EXECUTION_TIME(0) */ SLEEP(60)",
                    "SELECT BENCHMARK(10000000000, MD5('x'))",
                    NUMBERS_TO_1000 + "SELECT COUNT(*) FROM n a, n b, n c, n d"
                })
        void run_slowQuery_timesOut(String sql) {
            long startedAt = System.nanoTime();

            ExecutionOutcome outcome = run(sql);

            assertThat(outcome).isInstanceOf(TimedOut.class);
            assertThat(Duration.ofNanos(System.nanoTime() - startedAt))
                    .isLessThan(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("CTE đệ quy không điểm dừng bị MySQL cắt ở giới hạn đệ quy")
        void run_endlessRecursion_isStopped() {
            ExecutionOutcome outcome =
                    run(
                            "WITH RECURSIVE r(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM r)"
                                    + " SELECT COUNT(*) FROM r");

            assertThat(outcome).isInstanceOfAny(Failed.class, TimedOut.class);
        }
    }

    private static ExecutionOutcome run(String sql) {
        return sandbox.run(
                new QueryRun(SandboxFixtures.company(), sql, SandboxFixtures.FAST_LIMITS));
    }

    private static long countAsRoot(String sql) throws SQLException {
        try (Connection connection = rootConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static void executeAsRoot(String sql) throws SQLException {
        try (Connection connection = rootConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static Connection rootConnection() throws SQLException {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword());
    }
}
