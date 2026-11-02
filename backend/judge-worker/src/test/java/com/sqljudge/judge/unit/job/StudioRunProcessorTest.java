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
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.job.RunStatus;
import com.sqljudge.judge.job.StudioRunProcessor;
import com.sqljudge.judge.job.StudioRunRequest;
import com.sqljudge.judge.job.StudioRunResult;
import com.sqljudge.judge.job.VariantRunResult;
import com.sqljudge.judge.sandbox.Dialect;
import com.sqljudge.judge.sandbox.LogicalType;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StudioRunProcessorTest {

    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID PUBLIC_VARIANT = UUID.randomUUID();
    private static final UUID HIDDEN_VARIANT = UUID.randomUUID();
    private static final String CHECKSUM = "abc123";
    private static final DatasetSnapshotResponse SNAPSHOT =
            new DatasetSnapshotResponse(
                    VERSION_ID,
                    CHECKSUM,
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
                                                    List.of(List.of("1", "Kỹ thuật"))))),
                            new SnapshotVariant(
                                    HIDDEN_VARIANT,
                                    "Ẩn",
                                    false,
                                    List.of(
                                            new SnapshotVariantTable(
                                                    "departments",
                                                    List.of(
                                                            List.of("1", "Kế toán"),
                                                            List.of("2", "Pháp chế")))))));

    private final List<QueryRun> runs = new ArrayList<>();
    private int snapshotFetches;

    @Test
    @DisplayName("Chạy SQL trên từng bộ dữ liệu, trả bảng xem trước kèm số dòng")
    void process_validQuery_runsEveryVariant() {
        StudioRunProcessor processor =
                processor(
                        run ->
                                new ExecutionOutcome.Succeeded(
                                        tableOfDepartments(run), runs.size() * 10L));

        StudioRunResult result = processor.process(request("SELECT name FROM departments"));

        assertThat(result.rejection()).isNull();
        assertThat(result.systemError()).isNull();
        assertThat(result.variants())
                .extracting(VariantRunResult::variantName)
                .containsExactly("Mẫu", "Ẩn");
        VariantRunResult hidden = result.variants().get(1);
        assertThat(hidden.isPublic()).isFalse();
        assertThat(hidden.status()).isEqualTo(RunStatus.SUCCEEDED);
        assertThat(hidden.columns()).containsExactly("name");
        assertThat(hidden.rows()).containsExactly(List.of("Kế toán"), List.of("Pháp chế"));
        assertThat(hidden.rowCount()).isEqualTo(2);
        assertThat(hidden.truncated()).isFalse();
        assertThat(runs.get(0).dataset().tables().get(0).rows())
                .containsExactly(List.of("1", "Kỹ thuật"));
        assertThat(runs.get(0).limits().timeout().toMillis()).isEqualTo(2000);
    }

    @Test
    @DisplayName("Lệnh bị guard chặn thì trả lý do ngay, không tải dataset, không chạy sandbox")
    void process_forbiddenStatement_isRejectedEarly() {
        StudioRunProcessor processor = processor(run -> succeeded(List.of()));

        StudioRunResult result = processor.process(request("DROP TABLE departments"));

        assertThat(result.rejection().verdict()).isEqualTo(Verdict.FS);
        assertThat(result.rejection().message()).isEqualTo("Bài này chỉ cho phép SELECT, WITH");
        assertThat(result.variants()).isEmpty();
        assertThat(snapshotFetches).isZero();
        assertThat(runs).isEmpty();
    }

    @Test
    @DisplayName("Kết quả dài chỉ giữ 100 dòng đầu và cắt ô quá dài")
    void process_largeResult_isTruncated() {
        List<List<Object>> manyRows =
                IntStream.rangeClosed(1, 150)
                        .<List<Object>>mapToObj(index -> List.of("x".repeat(600)))
                        .toList();
        StudioRunProcessor processor = processor(run -> succeeded(manyRows));

        VariantRunResult first =
                processor.process(request("SELECT name FROM departments")).variants().get(0);

        assertThat(first.rowCount()).isEqualTo(150);
        assertThat(first.rows()).hasSize(100);
        assertThat(first.truncated()).isTrue();
        assertThat((String) first.rows().get(0).get(0)).hasSize(501).endsWith("…");
    }

    @Test
    @DisplayName("Lỗi SQL, quá giờ và lỗi sandbox được ghi theo từng bộ dữ liệu")
    void process_failures_areReportedPerVariant() {
        List<ExecutionOutcome> outcomes =
                List.of(
                        new ExecutionOutcome.Failed(
                                new SanitizedMessage("column \"nam\" does not exist"), 3),
                        new ExecutionOutcome.TimedOut(2000));
        StudioRunProcessor processor = processor(run -> outcomes.get(runs.size() - 1));

        List<VariantRunResult> variants =
                processor.process(request("SELECT nam FROM departments")).variants();

        assertThat(variants.get(0).status()).isEqualTo(RunStatus.FAILED);
        assertThat(variants.get(0).message()).isEqualTo("column \"nam\" does not exist");
        assertThat(variants.get(1).status()).isEqualTo(RunStatus.TIMED_OUT);
        assertThat(variants.get(1).runtimeMs()).isEqualTo(2000);
    }

    @Test
    @DisplayName("Không tải được dataset thì báo lỗi hệ thống, không chạy sandbox")
    void process_snapshotUnavailable_reportsSystemError() {
        StudioRunProcessor processor =
                new StudioRunProcessor(
                        new StatementGuard(),
                        (versionId, checksum) -> {
                            throw new SnapshotUnavailableException("problem-service down", null);
                        },
                        new SandboxRegistry(Map.of()));

        StudioRunResult result = processor.process(request("SELECT name FROM departments"));

        assertThat(result.systemError()).isEqualTo("snapshot_unavailable");
        assertThat(result.variants()).isEmpty();
    }

    private StudioRunProcessor processor(Function<QueryRun, ExecutionOutcome> sandbox) {
        return new StudioRunProcessor(
                new StatementGuard(),
                (versionId, checksum) -> {
                    snapshotFetches++;
                    assertThat(versionId).isEqualTo(VERSION_ID);
                    assertThat(checksum).isEqualTo(CHECKSUM);
                    return SNAPSHOT;
                },
                new SandboxRegistry(
                        Map.of(
                                Dialect.POSTGRESQL,
                                run -> {
                                    runs.add(run);
                                    return sandbox.apply(run);
                                })));
    }

    private static StudioRunRequest request(String sql) {
        return new StudioRunRequest(
                UUID.randomUUID(), VERSION_ID, CHECKSUM, Dialect.POSTGRESQL, sql, 2000);
    }

    private static ResultTable tableOfDepartments(QueryRun run) {
        List<List<Object>> names =
                run.dataset().tables().get(0).rows().stream()
                        .<List<Object>>map(row -> List.of(row.get(1)))
                        .toList();
        return new ResultTable(List.of("name"), names);
    }

    private static ExecutionOutcome succeeded(List<List<Object>> rows) {
        return new ExecutionOutcome.Succeeded(new ResultTable(List.of("name"), rows), 5);
    }
}
