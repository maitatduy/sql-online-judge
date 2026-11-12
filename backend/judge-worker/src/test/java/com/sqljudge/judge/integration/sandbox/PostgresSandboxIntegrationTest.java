package com.sqljudge.judge.integration.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.sandbox.PostgresSandbox;
import com.sqljudge.judge.sandbox.PostgresSandboxConfig;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.RunNames;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

class PostgresSandboxIntegrationTest {

    private static final String ADMIN_USER = "judge_admin";
    private static final String ADMIN_PASSWORD = "admin-secret";
    private static final Path INIT_SCRIPT =
            Path.of("..", "..", "infra", "sandbox", "postgres", "init", "01-create-judge-admin.sh");
    private static final int EXECUTABLE = 0755;
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16.9-alpine")
                    .withDatabaseName("sandbox")
                    .withEnv("SANDBOX_PG_ADMIN_USER", ADMIN_USER)
                    .withEnv("SANDBOX_PG_ADMIN_PASSWORD", ADMIN_PASSWORD)
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(INIT_SCRIPT, EXECUTABLE),
                            "/docker-entrypoint-initdb.d/01-create-judge-admin.sh");

    private static PostgresSandbox sandbox;

    @BeforeAll
    static void startSandbox() {
        POSTGRES.start();
        sandbox =
                new PostgresSandbox(
                        new PostgresSandboxConfig(
                                POSTGRES.getJdbcUrl(), ADMIN_USER, ADMIN_PASSWORD),
                        Clock.systemUTC());
    }

    @Test
    @DisplayName("Dọn schema và role của lần chạy cũ hơn mốc cắt, giữ lần chạy mới và schema khác")
    void removeRunsCreatedBefore_staleRuns_areDropped() throws SQLException {
        Instant now = Instant.now();
        String stale = RunNames.newRunName(now.minus(Duration.ofHours(1)));
        String staleRoleOnly = RunNames.newRunName(now.minus(Duration.ofHours(1)));
        String fresh = RunNames.newRunName(now);
        executeAsAdmin(
                "CREATE SCHEMA " + stale,
                "CREATE ROLE " + stale,
                "CREATE ROLE " + staleRoleOnly,
                "CREATE SCHEMA " + fresh,
                "CREATE SCHEMA sb_keep");
        try {
            int removed = sandbox.removeRunsCreatedBefore(now.minus(Duration.ofMinutes(10)));

            assertThat(removed).isEqualTo(2);
            assertThat(sandboxObjectNames()).containsExactlyInAnyOrder(fresh, "sb_keep");
        } finally {
            executeAsAdmin("DROP SCHEMA IF EXISTS " + fresh, "DROP SCHEMA IF EXISTS sb_keep");
        }
    }

    @AfterEach
    void assertNothingLeftBehind() throws SQLException {
        assertThat(
                        countAsSuperuser(
                                "SELECT COUNT(*) FROM pg_namespace WHERE nspname LIKE 'sb\\_%'"))
                .isZero();
        assertThat(countAsSuperuser("SELECT COUNT(*) FROM pg_roles WHERE rolname LIKE 'sb\\_%'"))
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
        assertThat(table.rows())
                .containsExactly(
                        Arrays.asList(
                                "Hoàng Quốc Bảo",
                                new BigDecimal("48000000.50"),
                                "Kỹ thuật",
                                LocalDate.of(2024, 3, 1),
                                true,
                                LocalDateTime.of(2026, 1, 5, 9, 30)),
                        Arrays.asList(
                                "Mai Lan",
                                new BigDecimal("30000000"),
                                null,
                                LocalDate.of(2025, 7, 15),
                                false,
                                null));
    }

    @Test
    @DisplayName("Lỗi cú pháp trả Failed, thông báo không lộ tên schema tạm")
    void run_syntaxError_returnsSanitizedFailure() {
        ExecutionOutcome outcome = run("SELECT nam FROM employees");

        assertThat(outcome).isInstanceOf(Failed.class);
        String message = ((Failed) outcome).message().text();
        assertThat(message).contains("nam").doesNotContain("sb_").doesNotContain("ERROR:");
    }

    @Test
    @DisplayName("Kết quả nhiều hơn giới hạn dòng thì dừng đọc và báo lỗi")
    void run_tooManyRows_returnsFailure() {
        ExecutionOutcome outcome =
                run(
                        "SELECT a.i FROM generate_series(1, 1000) a(i),"
                                + " generate_series(1, 1000) b(i)");

        assertThat(outcome).isInstanceOf(Failed.class);
        assertThat(((Failed) outcome).message().text()).isEqualTo("Kết quả có hơn 100 dòng");
    }

    @Test
    @DisplayName("Kết quả vượt dung lượng cho phép thì dừng đọc và báo lỗi")
    void run_resultTooLarge_returnsFailure() {
        ExecutionOutcome outcome = run("SELECT repeat('x', 20000) FROM generate_series(1, 10)");

        assertThat(outcome).isInstanceOf(Failed.class);
        assertThat(((Failed) outcome).message().text())
                .isEqualTo("Kết quả vượt quá dung lượng cho phép");
    }

    @Test
    @DisplayName("Câu lệnh không trả về bảng kết quả thì báo lỗi")
    void run_statementWithoutResult_returnsFailure() {
        ExecutionOutcome outcome = run("SET work_mem = '64kB'");

        assertThat(outcome).isInstanceOf(Failed.class);
        assertThat(((Failed) outcome).message().text())
                .isEqualTo("Câu lệnh không trả về bảng kết quả");
    }

    @Nested
    @DisplayName("Bộ truy vấn tấn công")
    class Attacks {

        @ParameterizedTest
        @DisplayName("Lệnh phá dữ liệu, cấu trúc, quyền và đọc file đều bị DB chặn")
        @ValueSource(
                strings = {
                    "DROP TABLE employees",
                    "TRUNCATE TABLE employees",
                    "DELETE FROM employees",
                    "UPDATE employees SET salary = 0",
                    "CREATE TABLE stolen (id int)",
                    "CREATE TEMP TABLE stolen AS SELECT * FROM employees",
                    "DROP DATABASE sandbox",
                    "CREATE ROLE hacker SUPERUSER",
                    "SET ROLE judge_admin",
                    "CREATE EXTENSION dblink",
                    "SELECT pg_read_file('/etc/passwd')",
                    "SELECT lo_import('/etc/passwd')",
                    "COPY employees TO PROGRAM 'id'",
                    "COPY (SELECT 1) TO '/tmp/leak.csv'",
                    "SELECT 1; DROP TABLE employees"
                })
        void run_destructiveStatement_isBlocked(String sql) {
            assertThat(run(sql)).isInstanceOf(Failed.class);
        }

        @Test
        @DisplayName("Không đọc được schema của lần chạy khác hay schema public")
        void run_otherSchemas_areNotReadable() throws SQLException {
            executeAsSuperuser(
                    "CREATE SCHEMA other_run;"
                            + " CREATE TABLE other_run.answers (secret text);"
                            + " CREATE TABLE public.answers (secret text)");
            try {
                assertThat(run("SELECT * FROM other_run.answers")).isInstanceOf(Failed.class);
                assertThat(run("SELECT * FROM public.answers")).isInstanceOf(Failed.class);
                ExecutionOutcome visible =
                        run(
                                "SELECT table_name FROM information_schema.tables"
                                        + " WHERE table_schema NOT IN"
                                        + " ('pg_catalog', 'information_schema')"
                                        + " ORDER BY table_name");
                assertThat(((Succeeded) visible).table().rows())
                        .containsExactly(List.of("departments"), List.of("employees"));
            } finally {
                executeAsSuperuser("DROP SCHEMA other_run CASCADE; DROP TABLE public.answers");
            }
        }

        @ParameterizedTest
        @DisplayName("Câu chạy quá lâu bị dừng theo giới hạn thời gian, không treo worker")
        @ValueSource(
                strings = {
                    "SELECT pg_sleep(60)",
                    "SELECT set_config('statement_timeout', '0', false), pg_sleep(60)",
                    "WITH RECURSIVE r(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM r)"
                            + " SELECT COUNT(*) FROM r",
                    "SELECT COUNT(*) FROM generate_series(1, 100000) a,"
                            + " generate_series(1, 100000) b"
                })
        void run_slowQuery_timesOut(String sql) {
            long startedAt = System.nanoTime();

            ExecutionOutcome outcome = run(sql);

            assertThat(outcome).isInstanceOf(TimedOut.class);
            assertThat(Duration.ofNanos(System.nanoTime() - startedAt))
                    .isLessThan(Duration.ofSeconds(10));
        }
    }

    private static ExecutionOutcome run(String sql) {
        return sandbox.run(
                new QueryRun(SandboxFixtures.company(), sql, SandboxFixtures.FAST_LIMITS));
    }

    private static long countAsSuperuser(String sql) throws SQLException {
        try (Connection connection = POSTGRES.createConnection("");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static List<String> sandboxObjectNames() throws SQLException {
        List<String> names = new ArrayList<>();
        try (Connection connection = POSTGRES.createConnection("");
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "SELECT nspname FROM pg_namespace WHERE nspname LIKE 'sb\\_%' UNION"
                                        + " ALL SELECT rolname FROM pg_roles WHERE rolname LIKE"
                                        + " 'sb\\_%'")) {
            while (resultSet.next()) {
                names.add(resultSet.getString(1));
            }
        }
        return names;
    }

    private static void executeAsAdmin(String... statements) throws SQLException {
        try (Connection connection =
                        DriverManager.getConnection(
                                POSTGRES.getJdbcUrl(), ADMIN_USER, ADMIN_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    private static void executeAsSuperuser(String sql) throws SQLException {
        try (Connection connection = POSTGRES.createConnection("");
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
