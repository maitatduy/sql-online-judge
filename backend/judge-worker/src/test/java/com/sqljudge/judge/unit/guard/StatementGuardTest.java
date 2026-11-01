package com.sqljudge.judge.unit.guard;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.compare.SanitizedMessage;
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.guard.GuardDecision;
import com.sqljudge.judge.guard.GuardPolicy;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.guard.StatementType;
import java.util.EnumSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StatementGuardTest {

    private static final GuardPolicy SELECT_ONLY = GuardPolicy.selectOnly();
    private static final GuardPolicy UPDATE_PROBLEM =
            GuardPolicy.of(
                    EnumSet.of(StatementType.SELECT, StatementType.WITH, StatementType.UPDATE));
    private static final String SELECT_ONLY_MESSAGE = "Bài này chỉ cho phép SELECT, WITH";

    private final StatementGuard guard = new StatementGuard();

    private static GuardDecision forbidden(String message) {
        return new GuardDecision.Rejected(Verdict.FS, new SanitizedMessage(message));
    }

    @Nested
    @DisplayName("Bài chỉ cho SELECT")
    class SelectOnly {

        @ParameterizedTest
        @DisplayName("Cho phép một câu SELECT hoặc WITH, kể cả cú pháp riêng của từng dialect")
        @ValueSource(
                strings = {
                    "SELECT name FROM departments",
                    "SELECT name FROM departments;",
                    "SELECT salary::numeric FROM employees WHERE full_name ILIKE '%an%'",
                    "SELECT `name` FROM `departments` LIMIT 5, 10",
                    "SELECT full_name || ' - ' || strftime('%Y', hired_at) FROM employees",
                    "WITH top AS (SELECT * FROM employees) SELECT * FROM top",
                    "WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 5)"
                            + " SELECT i FROM n",
                    "SELECT 1 UNION SELECT 2",
                    "SELECT 1 -- DROP TABLE employees",
                    "SELECT 1 /* ; DROP TABLE employees; */"
                })
        void check_singleQuery_isAllowed(String sql) {
            assertThat(guard.check(sql, SELECT_ONLY)).isEqualTo(new GuardDecision.Allowed());
        }

        @ParameterizedTest
        @DisplayName("Lệnh thay đổi dữ liệu, cấu trúc hoặc quyền bị chặn với FS")
        @ValueSource(
                strings = {
                    "DELETE FROM employees",
                    "UPDATE employees SET salary = 0",
                    "INSERT INTO departments VALUES (9, 'X')",
                    "DROP TABLE employees",
                    "DROP DATABASE sqljudge",
                    "TRUNCATE TABLE employees",
                    "CREATE ROLE hacker",
                    "GRANT ALL ON employees TO PUBLIC",
                    "ALTER TABLE employees ADD COLUMN x INT",
                    "SELECT * INTO copy_of_employees FROM employees",
                    "WITH gone AS (DELETE FROM employees RETURNING *) SELECT * FROM gone",
                    "COPY employees TO PROGRAM 'id'",
                    "LOAD DATA INFILE '/etc/passwd' INTO TABLE employees",
                    "ATTACH DATABASE '/tmp/other.db' AS other",
                    "PRAGMA writable_schema = 1",
                    "/* ẩn */ drop table employees"
                })
        void check_forbiddenStatement_returnsForbidden(String sql) {
            assertThat(guard.check(sql, SELECT_ONLY)).isEqualTo(forbidden(SELECT_ONLY_MESSAGE));
        }

        @Test
        @DisplayName("Lệnh cấm nằm sau dấu chấm phẩy và comment vẫn bị chặn")
        void check_forbiddenStatementAfterComment_returnsForbidden() {
            String sql = "SELECT 1;\n-- chỉ là comment\nDROP TABLE employees";

            assertThat(guard.check(sql, SELECT_ONLY)).isEqualTo(forbidden(SELECT_ONLY_MESSAGE));
        }

        @ParameterizedTest
        @DisplayName("Lệnh cấm parser không đọc được vẫn bị chặn khi nối sau câu SELECT")
        @ValueSource(
                strings = {
                    "SELECT 1; CREATE ROLE hacker",
                    "SELECT 1; GRANT ALL ON employees TO PUBLIC"
                })
        void check_unparsableForbiddenAfterQuery_returnsForbidden(String sql) {
            assertThat(guard.check(sql, SELECT_ONLY)).isEqualTo(forbidden(SELECT_ONLY_MESSAGE));
        }

        @ParameterizedTest
        @DisplayName("Hai câu lệnh trong bài chỉ cho một câu bị chặn")
        @ValueSource(strings = {"SELECT 1; SELECT 2", "SELECT 1; SELEC 2"})
        void check_twoStatements_returnsForbidden(String sql) {
            assertThat(guard.check(sql, SELECT_ONLY))
                    .isEqualTo(forbidden("Bài này chỉ cho phép 1 câu lệnh"));
        }

        @ParameterizedTest
        @DisplayName("Không có câu lệnh nào thì trả RE")
        @ValueSource(strings = {"", "   ", "-- chỉ có comment", "/* trống */", ";"})
        void check_noStatement_returnsRuntimeError(String sql) {
            assertThat(guard.check(sql, SELECT_ONLY))
                    .isEqualTo(
                            new GuardDecision.Rejected(
                                    Verdict.RE, new SanitizedMessage("Chưa có câu lệnh nào")));
        }

        @ParameterizedTest
        @DisplayName("Câu parser không đọc được thì giao cho sandbox chạy, không chặn nhầm")
        @ValueSource(
                strings = {
                    "SELEC name FROM departments",
                    "SELECT name FROM departments WHERE id >",
                    "(SELECT name FROM departments"
                })
        void check_unparsableSql_isUnverified(String sql) {
            assertThat(guard.check(sql, SELECT_ONLY)).isEqualTo(new GuardDecision.Unverified());
        }
    }

    @Nested
    @DisplayName("Bài cho phép UPDATE")
    class UpdateProblem {

        @Test
        @DisplayName("Cho phép nhiều câu lệnh trong danh sách của đề")
        void check_allowedStatements_isAllowed() {
            String sql =
                    "UPDATE employees SET salary = salary * 1.1 WHERE department_id = 1;"
                            + " WITH raised AS (UPDATE employees SET salary = 0 RETURNING id)"
                            + " SELECT COUNT(*) FROM raised;"
                            + " SELECT * FROM employees";

            assertThat(guard.check(sql, UPDATE_PROBLEM)).isEqualTo(new GuardDecision.Allowed());
        }

        @ParameterizedTest
        @DisplayName("Lệnh ngoài danh sách của đề bị chặn và báo đủ các lệnh được dùng")
        @ValueSource(
                strings = {
                    "DELETE FROM employees",
                    "INSERT INTO departments VALUES (9, 'X')",
                    "WITH added AS (INSERT INTO departments VALUES (9, 'X') RETURNING id)"
                            + " SELECT * FROM added",
                    "UPDATE employees SET salary = 0;"
                            + " WITH gone AS (DELETE FROM employees RETURNING id)"
                            + " UPDATE departments SET name = 'X'"
                })
        void check_statementOutsidePolicy_returnsForbidden(String sql) {
            assertThat(guard.check(sql, UPDATE_PROBLEM))
                    .isEqualTo(forbidden("Bài này chỉ cho phép SELECT, WITH, UPDATE"));
        }

        @Test
        @DisplayName("Quá 20 câu lệnh thì bị chặn")
        void check_tooManyStatements_returnsForbidden() {
            String sql =
                    IntStream.rangeClosed(1, 21)
                            .mapToObj(index -> "SELECT " + index)
                            .collect(Collectors.joining("; "));

            assertThat(guard.check(sql, UPDATE_PROBLEM))
                    .isEqualTo(forbidden("Bài này cho phép tối đa 20 câu lệnh"));
        }
    }

    @Test
    @DisplayName("Đề chỉ có SELECT, WITH được giới hạn 1 câu lệnh, có lệnh DML thì 20")
    void policy_maxStatements_dependsOnAllowedStatements() {
        assertThat(SELECT_ONLY.maxStatements()).isEqualTo(1);
        assertThat(GuardPolicy.of(EnumSet.of(StatementType.SELECT)).maxStatements()).isEqualTo(1);
        assertThat(UPDATE_PROBLEM.maxStatements()).isEqualTo(20);
    }
}
