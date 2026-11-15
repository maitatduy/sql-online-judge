package com.sqljudge.judge.integration.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotColumn;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotTable;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariant;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariantTable;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.job.CodeRunResult;
import com.sqljudge.judge.job.LessonRunProcessor;
import com.sqljudge.judge.job.LessonRunRequest;
import com.sqljudge.judge.job.RunOutcome;
import com.sqljudge.judge.sandbox.Dialect;
import com.sqljudge.judge.sandbox.LogicalType;
import com.sqljudge.judge.sandbox.PostgresSandbox;
import com.sqljudge.judge.sandbox.PostgresSandboxConfig;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

class LessonRunAttackIntegrationTest {

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
    private static final UUID PUBLIC_VARIANT = UUID.randomUUID();
    private static final UUID HIDDEN_VARIANT = UUID.randomUUID();
    private static final String SECRET = "Bí mật";
    private static final String NAMES = "SELECT name FROM departments ORDER BY id";
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
                                    PUBLIC_VARIANT,
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
                                                    List.of(List.of("1", SECRET)))))));

    private static LessonRunProcessor processor;

    @BeforeAll
    static void startSandbox() {
        POSTGRES.start();
        PostgresSandbox sandbox =
                new PostgresSandbox(
                        new PostgresSandboxConfig(
                                POSTGRES.getJdbcUrl(), ADMIN_USER, ADMIN_PASSWORD),
                        Clock.systemUTC());
        processor =
                new LessonRunProcessor(
                        new StatementGuard(),
                        (versionId, checksum) -> SNAPSHOT,
                        new SandboxRegistry(Map.of(Dialect.POSTGRESQL, sandbox)));
    }

    @Test
    @DisplayName("Câu SELECT trong ô bài giảng trả bảng kết quả của variant công khai")
    void run_select_returnsPublicRows() {
        CodeRunResult result = run(NAMES, PUBLIC_VARIANT);

        assertThat(result.outcome()).isEqualTo(RunOutcome.OK);
        assertThat(result.rows()).containsExactly(List.of("Kỹ thuật"), List.of("Kinh doanh"));
    }

    @Test
    @DisplayName("Yêu cầu chạy trên variant ẩn bị từ chối, không lộ dữ liệu ẩn")
    void run_hiddenVariant_isRefused() {
        CodeRunResult result = run(NAMES, HIDDEN_VARIANT);

        assertThat(result.outcome()).isEqualTo(RunOutcome.SE);
        assertThat(result.rows()).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("Truy vấn tấn công bị chặn hoặc vô hại và không làm hỏng lần chạy sau")
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
    void run_attack_isContainedAndLeavesDataIntact(String attack) {
        CodeRunResult result = run(attack, PUBLIC_VARIANT);

        assertThat(result.outcome()).isNotEqualTo(RunOutcome.SE);
        assertThat(result.rows().size()).isLessThanOrEqualTo(100);
        assertThat(result.rows().toString()).doesNotContain(SECRET).doesNotContain("sb_");
        assertThat(String.valueOf(result.message())).doesNotContain(SECRET).doesNotContain("sb_");
        assertThat(run(NAMES, PUBLIC_VARIANT).rows())
                .containsExactly(List.of("Kỹ thuật"), List.of("Kinh doanh"));
    }

    private static CodeRunResult run(String sql, UUID variantId) {
        return processor.run(
                new LessonRunRequest(
                        UUID.randomUUID(),
                        VERSION_ID,
                        variantId,
                        "abc123",
                        Dialect.POSTGRESQL,
                        sql,
                        1000));
    }
}
