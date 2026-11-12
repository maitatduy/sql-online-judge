package com.sqljudge.judge.integration.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotColumn;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotTable;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariant;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariantTable;
import com.sqljudge.judge.compare.Cell.TextCell;
import com.sqljudge.judge.compare.CompareOptions;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.Row;
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.guard.StatementType;
import com.sqljudge.judge.job.JudgePackage;
import com.sqljudge.judge.job.JudgePackage.JudgeCase;
import com.sqljudge.judge.job.SubmissionJudgeProcessor;
import com.sqljudge.judge.job.SubmissionJudgeRequest;
import com.sqljudge.judge.job.SubmissionJudgement;
import com.sqljudge.judge.sandbox.Dialect;
import com.sqljudge.judge.sandbox.LogicalType;
import com.sqljudge.judge.sandbox.PostgresSandbox;
import com.sqljudge.judge.sandbox.PostgresSandboxConfig;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

class SubmissionJudgeAttackIntegrationTest {

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
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID SAMPLE_VARIANT = UUID.randomUUID();
    private static final UUID HIDDEN_VARIANT = UUID.randomUUID();
    private static final String CORRECT = "SELECT name FROM departments ORDER BY id";
    private static final DatasetSnapshotResponse SNAPSHOT =
            new DatasetSnapshotResponse(
                    VERSION_ID,
                    "abc123",
                    List.of(
                            new SnapshotTable(
                                    "departments",
                                    List.of(
                                            new SnapshotColumn(
                                                    "id", LogicalType.INTEGER, true, false, null),
                                            new SnapshotColumn(
                                                    "name",
                                                    LogicalType.TEXT,
                                                    false,
                                                    false,
                                                    null)))),
                    List.of(
                            new SnapshotVariant(
                                    SAMPLE_VARIANT,
                                    "Mẫu",
                                    true,
                                    List.of(
                                            new SnapshotVariantTable(
                                                    "departments",
                                                    List.of(
                                                            List.of("1", "Kỹ thuật"),
                                                            List.of("2", "Kinh doanh"))))),
                            new SnapshotVariant(
                                    HIDDEN_VARIANT,
                                    "Ẩn",
                                    false,
                                    List.of(
                                            new SnapshotVariantTable(
                                                    "departments",
                                                    List.of(List.of("1", "Bí mật")))))));

    private static SubmissionJudgeProcessor processor;

    @BeforeAll
    static void startSandbox() {
        POSTGRES.start();
        PostgresSandbox sandbox =
                new PostgresSandbox(
                        new PostgresSandboxConfig(
                                POSTGRES.getJdbcUrl(), ADMIN_USER, ADMIN_PASSWORD),
                        Clock.systemUTC());
        processor =
                new SubmissionJudgeProcessor(
                        new StatementGuard(),
                        (versionId, checksum) -> SNAPSHOT,
                        new SandboxRegistry(Map.of(Dialect.POSTGRESQL, sandbox)));
    }

    @Test
    @DisplayName("Lời giải đúng được AC trên cả testcase công khai lẫn testcase ẩn")
    void judge_correctSolution_isAccepted() {
        assertThat(judge(CORRECT).verdict()).isEqualTo(Verdict.AC);
    }

    @ParameterizedTest
    @DisplayName("Truy vấn tấn công không bao giờ được AC và không làm hỏng lần chấm sau")
    @ValueSource(
            strings = {
                "DROP TABLE departments",
                "TRUNCATE departments",
                "DROP DATABASE sandbox",
                "CREATE ROLE intruder",
                "SELECT pg_read_file('/etc/passwd')",
                "COPY departments TO PROGRAM 'id'",
                "SELECT table_schema, table_name FROM information_schema.tables",
                "SELECT nspname FROM pg_namespace",
                "SELECT pg_sleep(60)",
                "WITH RECURSIVE r(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM r)"
                        + " SELECT COUNT(*) FROM r",
                "SELECT * FROM generate_series(1, 5000000)",
                "SELECT name FROM departments; DROP TABLE departments"
            })
    void judge_attack_isNeverAcceptedAndLeavesDataIntact(String attack) {
        SubmissionJudgement judgement = judge(attack);

        assertThat(judgement.verdict()).isNotEqualTo(Verdict.AC);
        assertThat(judgement.verdict()).isNotEqualTo(Verdict.SE);
        assertThat(String.valueOf(judgement.message()))
                .doesNotContain("Bí mật")
                .doesNotContain("sb_");
        assertThat(judge(CORRECT).verdict()).isEqualTo(Verdict.AC);
    }

    @Test
    @DisplayName("Lỗi chỉ xảy ra trên dữ liệu ẩn thì RE mà không lộ giá trị ẩn qua thông báo")
    void judge_errorOnHiddenData_doesNotLeakIt() {
        SubmissionJudgement judgement =
                judge(
                        "SELECT CASE WHEN name = 'Bí mật' THEN CAST(name AS INTEGER)::text"
                                + " ELSE name END AS name FROM departments ORDER BY id");

        assertThat(judgement.verdict()).isEqualTo(Verdict.RE);
        assertThat(judgement.failedIndex()).isEqualTo(2);
        assertThat(judgement.failedIsHidden()).isTrue();
        assertThat(judgement.message()).isNull();
    }

    private static SubmissionJudgement judge(String code) {
        JudgePackage judgePackage =
                new JudgePackage(
                        VERSION_ID,
                        "abc123",
                        new CompareOptions(true, false, CompareOptions.DEFAULT_TOLERANCE),
                        1000,
                        Set.of(StatementType.SELECT, StatementType.WITH),
                        List.of(
                                new JudgeCase(
                                        1, SAMPLE_VARIANT, true, names("Kỹ thuật", "Kinh doanh")),
                                new JudgeCase(2, HIDDEN_VARIANT, false, names("Bí mật"))));
        return processor.judge(
                new SubmissionJudgeRequest(
                        UUID.randomUUID(), 1, Dialect.POSTGRESQL, code, judgePackage),
                progress -> {});
    }

    private static NormalizedTable names(String... values) {
        return new NormalizedTable(
                List.of("name"),
                java.util.Arrays.stream(values)
                        .map(value -> new Row(List.of(new TextCell(value))))
                        .toList());
    }
}
