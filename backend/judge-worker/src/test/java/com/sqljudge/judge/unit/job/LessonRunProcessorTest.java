package com.sqljudge.judge.unit.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotColumn;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotTable;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariant;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariantTable;
import com.sqljudge.judge.client.SnapshotUnavailableException;
import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.compare.SanitizedMessage;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.job.CodeRunResult;
import com.sqljudge.judge.job.LessonRunProcessor;
import com.sqljudge.judge.job.LessonRunRequest;
import com.sqljudge.judge.job.RunOutcome;
import com.sqljudge.judge.sandbox.Dialect;
import com.sqljudge.judge.sandbox.LogicalType;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.Sandbox;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LessonRunProcessorTest {

    private static final UUID RUN_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID PUBLIC_VARIANT = UUID.randomUUID();
    private static final UUID HIDDEN_VARIANT = UUID.randomUUID();
    private static final String CHECKSUM = "abc123";
    private static final String SQL = "SELECT id FROM departments";
    private static final DatasetSnapshotResponse SNAPSHOT =
            new DatasetSnapshotResponse(
                    VERSION_ID,
                    CHECKSUM,
                    List.of(
                            new SnapshotTable(
                                    "departments",
                                    List.of(
                                            new SnapshotColumn(
                                                    "id",
                                                    LogicalType.INTEGER,
                                                    true,
                                                    false,
                                                    null)))),
                    List.of(
                            variant(HIDDEN_VARIANT, false, "99"),
                            variant(PUBLIC_VARIANT, true, "1")));

    private final List<QueryRun> runs = new ArrayList<>();

    @Test
    @DisplayName("Ô bài giảng chỉ chạy trên đúng variant công khai được yêu cầu")
    void run_publicVariant_runsOnThatVariantOnly() {
        CodeRunResult result =
                run(
                        PUBLIC_VARIANT,
                        SQL,
                        query ->
                                new ExecutionOutcome.Succeeded(
                                        new ResultTable(List.of("id"), List.of(row(1))), 4));

        assertThat(result.runId()).isEqualTo(RUN_ID);
        assertThat(result.outcome()).isEqualTo(RunOutcome.OK);
        assertThat(result.columns()).containsExactly("id");
        assertThat(result.rows()).containsExactly(List.of("1"));
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).dataset().tables().get(0).rows()).containsExactly(List.of("1"));
        assertThat(runs.get(0).limits().timeout()).isEqualTo(Duration.ofMillis(2000));
    }

    @Test
    @DisplayName("Variant ẩn hoặc không có trong snapshot thì SE và không chạy SQL")
    void run_hiddenOrUnknownVariant_isRefusedWithoutRunning() {
        CodeRunResult hidden = run(HIDDEN_VARIANT, SQL, query -> null);
        CodeRunResult unknown = run(UUID.randomUUID(), SQL, query -> null);

        assertThat(hidden.outcome()).isEqualTo(RunOutcome.SE);
        assertThat(hidden.message()).isNull();
        assertThat(unknown.outcome()).isEqualTo(RunOutcome.SE);
        assertThat(runs).isEmpty();
    }

    @Test
    @DisplayName("Lệnh không phải SELECT hoặc nhiều câu lệnh thì FS mà không chạy")
    void run_forbiddenStatements_areRejectedWithoutRunning() {
        assertThat(run(PUBLIC_VARIANT, "DELETE FROM departments", query -> null).outcome())
                .isEqualTo(RunOutcome.FS);
        assertThat(run(PUBLIC_VARIANT, SQL + "; DROP TABLE departments", query -> null).outcome())
                .isEqualTo(RunOutcome.FS);
        assertThat(runs).isEmpty();
    }

    @Test
    @DisplayName("Lỗi SQL thành RE kèm thông báo, quá giờ thành TLE")
    void run_failures_mapToOutcomes() {
        CodeRunResult failed =
                run(
                        PUBLIC_VARIANT,
                        SQL,
                        query -> new ExecutionOutcome.Failed(new SanitizedMessage("sai cột"), 2));
        CodeRunResult timedOut =
                run(PUBLIC_VARIANT, SQL, query -> new ExecutionOutcome.TimedOut(2000));

        assertThat(failed.outcome()).isEqualTo(RunOutcome.RE);
        assertThat(failed.message()).isEqualTo("sai cột");
        assertThat(timedOut.outcome()).isEqualTo(RunOutcome.TLE);
    }

    @Test
    @DisplayName("Không tải được dataset thì SE")
    void run_snapshotUnavailable_isSystemError() {
        LessonRunProcessor processor =
                new LessonRunProcessor(
                        new StatementGuard(),
                        (versionId, checksum) -> {
                            throw new SnapshotUnavailableException("down", null);
                        },
                        new SandboxRegistry(Map.of()));

        assertThat(processor.run(request(PUBLIC_VARIANT, SQL)).outcome()).isEqualTo(RunOutcome.SE);
    }

    private CodeRunResult run(UUID variantId, String sql, Sandbox sandbox) {
        runs.clear();
        Sandbox recording =
                query -> {
                    runs.add(query);
                    return sandbox.run(query);
                };
        LessonRunProcessor processor =
                new LessonRunProcessor(
                        new StatementGuard(),
                        (versionId, checksum) -> SNAPSHOT,
                        new SandboxRegistry(Map.of(Dialect.POSTGRESQL, recording)));
        return processor.run(request(variantId, sql));
    }

    private static LessonRunRequest request(UUID variantId, String sql) {
        return new LessonRunRequest(
                RUN_ID, VERSION_ID, variantId, CHECKSUM, Dialect.POSTGRESQL, sql, 2000);
    }

    private static SnapshotVariant variant(UUID id, boolean isPublic, String departmentId) {
        return new SnapshotVariant(
                id,
                isPublic ? "Mẫu" : "Ẩn",
                isPublic,
                List.of(new SnapshotVariantTable("departments", List.of(List.of(departmentId)))));
    }

    private static List<Object> row(Object... values) {
        return Arrays.asList(values);
    }
}
