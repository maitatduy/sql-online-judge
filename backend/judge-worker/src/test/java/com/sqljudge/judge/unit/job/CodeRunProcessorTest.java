package com.sqljudge.judge.unit.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotColumn;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotTable;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariant;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariantTable;
import com.sqljudge.judge.client.SnapshotUnavailableException;
import com.sqljudge.judge.compare.CompareOptions;
import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.compare.SanitizedMessage;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.guard.StatementType;
import com.sqljudge.judge.job.CodeRunProcessor;
import com.sqljudge.judge.job.CodeRunRequest;
import com.sqljudge.judge.job.CodeRunResult;
import com.sqljudge.judge.job.JudgePackage;
import com.sqljudge.judge.job.JudgePackage.JudgeCase;
import com.sqljudge.judge.job.RunOutcome;
import com.sqljudge.judge.sandbox.Dialect;
import com.sqljudge.judge.sandbox.LogicalType;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.Sandbox;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CodeRunProcessorTest {

    private static final UUID RUN_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID HIDDEN_VARIANT = UUID.randomUUID();
    private static final UUID SAMPLE_VARIANT = UUID.randomUUID();
    private static final String CHECKSUM = "abc123";
    private static final String CODE = "SELECT id, name FROM departments";
    private static final NormalizedTable EMPTY = new NormalizedTable(List.of("id"), List.of());
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
                            new SnapshotVariant(
                                    HIDDEN_VARIANT,
                                    "Ẩn",
                                    false,
                                    List.of(
                                            new SnapshotVariantTable(
                                                    "departments", List.of(List.of("99"))))),
                            new SnapshotVariant(
                                    SAMPLE_VARIANT,
                                    "Mẫu",
                                    true,
                                    List.of(
                                            new SnapshotVariantTable(
                                                    "departments", List.of(List.of("1")))))));

    private final List<QueryRun> runs = new ArrayList<>();

    @Test
    @DisplayName(
            "Chạy thử chỉ chạy trên dataset của testcase công khai đầu tiên, giá trị thành chuỗi")
    void run_succeeds_onFirstPublicTestcaseOnly() {
        CodeRunResult result =
                run(
                        query ->
                                new ExecutionOutcome.Succeeded(
                                        new ResultTable(
                                                List.of("id", "name"),
                                                List.of(
                                                        row(new BigDecimal("1.50"), "A"),
                                                        row(2, null))),
                                        7),
                        CODE);

        assertThat(result.runId()).isEqualTo(RUN_ID);
        assertThat(result.outcome()).isEqualTo(RunOutcome.OK);
        assertThat(result.columns()).containsExactly("id", "name");
        assertThat(result.rows())
                .containsExactly(Arrays.asList("1.50", "A"), Arrays.asList("2", null));
        assertThat(result.isTruncated()).isFalse();
        assertThat(result.runtimeMs()).isEqualTo(7);
        assertThat(result.message()).isNull();
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).sql()).isEqualTo(CODE);
        assertThat(runs.get(0).dataset().tables().get(0).rows()).containsExactly(List.of("1"));
    }

    @Test
    @DisplayName("Kết quả quá 100 dòng thì chỉ giữ 100 dòng đầu và đánh dấu bị cắt")
    void run_manyRows_isTruncated() {
        List<List<Object>> rows = IntStream.range(0, 150).mapToObj(i -> row(i, "x")).toList();

        CodeRunResult result =
                run(
                        query ->
                                new ExecutionOutcome.Succeeded(
                                        new ResultTable(List.of("id", "name"), rows), 3),
                        CODE);

        assertThat(result.rows()).hasSize(CodeRunProcessor.MAX_ROWS);
        assertThat(result.isTruncated()).isTrue();
    }

    @Test
    @DisplayName("Lỗi SQL trả RE kèm thông báo, quá giờ trả TLE, lệnh bị cấm trả FS mà không chạy")
    void run_failures_mapToOutcomes() {
        CodeRunResult failed =
                run(
                        query ->
                                new ExecutionOutcome.Failed(
                                        new SanitizedMessage("cột x không có"), 2),
                        CODE);
        assertThat(failed.outcome()).isEqualTo(RunOutcome.RE);
        assertThat(failed.message()).isEqualTo("cột x không có");

        assertThat(run(query -> new ExecutionOutcome.TimedOut(2000), CODE).outcome())
                .isEqualTo(RunOutcome.TLE);

        CodeRunResult forbidden = run(query -> new ExecutionOutcome.TimedOut(1), "DROP TABLE x");
        assertThat(forbidden.outcome()).isEqualTo(RunOutcome.FS);
        assertThat(forbidden.message()).isEqualTo("Bài này chỉ cho phép SELECT, WITH");
        assertThat(runs).isEmpty();
    }

    @Test
    @DisplayName("Lỗi sandbox hay không tải được dataset thì SE, không kèm thông báo")
    void run_infrastructureFailures_areSystemErrors() {
        CodeRunResult sandboxFailed =
                run(query -> new ExecutionOutcome.SandboxFailed("lost", 1), CODE);
        assertThat(sandboxFailed.outcome()).isEqualTo(RunOutcome.SE);
        assertThat(sandboxFailed.message()).isNull();

        CodeRunProcessor unavailable =
                new CodeRunProcessor(
                        new StatementGuard(),
                        (versionId, checksum) -> {
                            throw new SnapshotUnavailableException("down", null);
                        },
                        new SandboxRegistry(Map.of()));
        assertThat(unavailable.run(request(CODE)).outcome()).isEqualTo(RunOutcome.SE);
    }

    private CodeRunResult run(Sandbox sandbox, String code) {
        runs.clear();
        Sandbox recording =
                query -> {
                    runs.add(query);
                    return sandbox.run(query);
                };
        CodeRunProcessor processor =
                new CodeRunProcessor(
                        new StatementGuard(),
                        (versionId, checksum) -> SNAPSHOT,
                        new SandboxRegistry(Map.of(Dialect.POSTGRESQL, recording)));
        return processor.run(request(code));
    }

    private static CodeRunRequest request(String code) {
        JudgePackage judgePackage =
                new JudgePackage(
                        VERSION_ID,
                        CHECKSUM,
                        new CompareOptions(false, false, CompareOptions.DEFAULT_TOLERANCE),
                        2000,
                        Set.of(StatementType.SELECT, StatementType.WITH),
                        List.of(
                                new JudgeCase(1, HIDDEN_VARIANT, false, EMPTY),
                                new JudgeCase(2, SAMPLE_VARIANT, true, EMPTY)));
        return new CodeRunRequest(RUN_ID, Dialect.POSTGRESQL, code, judgePackage);
    }

    private static List<Object> row(Object... values) {
        return Arrays.asList(values);
    }
}
