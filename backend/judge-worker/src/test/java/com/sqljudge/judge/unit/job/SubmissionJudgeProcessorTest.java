package com.sqljudge.judge.unit.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotColumn;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotTable;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariant;
import com.sqljudge.judge.client.SnapshotUnavailableException;
import com.sqljudge.judge.compare.Cell.NumberCell;
import com.sqljudge.judge.compare.Cell.TextCell;
import com.sqljudge.judge.compare.CompareOptions;
import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.MismatchReason;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.compare.Row;
import com.sqljudge.judge.compare.SanitizedMessage;
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.guard.StatementType;
import com.sqljudge.judge.job.ExpectedTable.CellKind;
import com.sqljudge.judge.job.ExpectedTable.WireCell;
import com.sqljudge.judge.job.JudgePackage;
import com.sqljudge.judge.job.JudgePackage.JudgeCase;
import com.sqljudge.judge.job.JudgeProgress;
import com.sqljudge.judge.job.ResultDiff;
import com.sqljudge.judge.job.SubmissionJudgeProcessor;
import com.sqljudge.judge.job.SubmissionJudgeRequest;
import com.sqljudge.judge.job.SubmissionJudgement;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubmissionJudgeProcessorTest {

    private static final UUID SUBMISSION_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID SAMPLE_VARIANT = UUID.randomUUID();
    private static final UUID HIDDEN_VARIANT = UUID.randomUUID();
    private static final UUID OTHER_HIDDEN_VARIANT = UUID.randomUUID();
    private static final String CHECKSUM = "abc123";
    private static final String CODE = "SELECT id, name FROM departments";
    private static final NormalizedTable TWO_ROWS =
            new NormalizedTable(
                    List.of("id", "name"),
                    List.of(
                            new Row(List.of(new NumberCell(BigDecimal.ONE), new TextCell("A"))),
                            new Row(
                                    List.of(
                                            new NumberCell(BigDecimal.valueOf(2)),
                                            new TextCell("B")))));
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
                            new SnapshotVariant(SAMPLE_VARIANT, "Mẫu", true, List.of()),
                            new SnapshotVariant(HIDDEN_VARIANT, "Ẩn", false, List.of()),
                            new SnapshotVariant(OTHER_HIDDEN_VARIANT, "Ẩn 2", false, List.of())));

    private final List<QueryRun> runs = new ArrayList<>();
    private final List<JudgeProgress> progress = new ArrayList<>();

    @Test
    @DisplayName("Mọi testcase khớp thì AC, báo tiến độ từng testcase và lấy thời gian lớn nhất")
    void judge_allTestcasesMatch_isAccepted() {
        SubmissionJudgement judgement =
                judge(
                        returning(
                                outcome(4, row(1, "A"), row(2, "B")),
                                outcome(9, row(1, "A"), row(2, "B")),
                                outcome(6, row(1, "A"), row(2, "B"))),
                        CODE,
                        false);

        assertThat(judgement.verdict()).isEqualTo(Verdict.AC);
        assertThat(judgement.runtimeMs()).isEqualTo(9);
        assertThat(judgement.failedIndex()).isNull();
        assertThat(judgement.judgedTestcases()).isEqualTo(3);
        assertThat(judgement.totalTestcases()).isEqualTo(3);
        assertThat(progress).extracting(JudgeProgress::judgedTestcases).containsExactly(1, 2, 3);
        assertThat(progress)
                .allSatisfy(item -> assertThat(item.submissionId()).isEqualTo(SUBMISSION_ID));
        assertThat(runs).extracting(run -> run.limits().timeout().toMillis()).containsOnly(2000L);
        assertThat(runs).extracting(QueryRun::sql).containsOnly(CODE);
    }

    @Test
    @DisplayName("Sai ở testcase ẩn thì WA, ghi số thứ tự và dừng, không chạy testcase sau")
    void judge_hiddenTestcaseDiffers_stopsAtFirstFailure() {
        SubmissionJudgement judgement =
                judge(
                        returning(
                                outcome(4, row(1, "A"), row(2, "B")),
                                outcome(5, row(1, "A")),
                                outcome(6, row(1, "A"), row(2, "B"))),
                        CODE,
                        false);

        assertThat(judgement.verdict()).isEqualTo(Verdict.WA);
        assertThat(judgement.failedIndex()).isEqualTo(2);
        assertThat(judgement.failedIsHidden()).isTrue();
        assertThat(judgement.diff()).isNull();
        assertThat(judgement.judgedTestcases()).isEqualTo(2);
        assertThat(runs).hasSize(2);
        assertThat(progress).extracting(JudgeProgress::judgedTestcases).containsExactly(1, 2);
    }

    @Test
    @DisplayName("Đúng dữ liệu nhưng sai thứ tự khi đề yêu cầu thứ tự thì OM ở testcase công khai")
    void judge_wrongOrderWhenOrdered_isOrderMismatch() {
        SubmissionJudgement judgement =
                judge(returning(outcome(4, row(2, "B"), row(1, "A"))), CODE, true);

        assertThat(judgement.verdict()).isEqualTo(Verdict.OM);
        assertThat(judgement.failedIndex()).isEqualTo(1);
        assertThat(judgement.failedIsHidden()).isFalse();
        ResultDiff diff = judgement.diff();
        assertThat(diff.reason()).isEqualTo(MismatchReason.ORDER);
        assertThat(diff.orderMismatch().rowNumber()).isEqualTo(1);
        assertThat(diff.orderMismatch().expected()).containsExactly(number("1"), text("A"));
        assertThat(diff.orderMismatch().actual()).containsExactly(number("2"), text("B"));
    }

    @Test
    @DisplayName("Sai dữ liệu ở testcase công khai thì kèm diff: dòng thiếu, dòng thừa, số dòng")
    void judge_publicTestcaseDiffers_revealsDiff() {
        SubmissionJudgement judgement =
                judge(returning(outcome(4, row(1, "A"), row(3, "C"), row(4, "D"))), CODE, false);

        assertThat(judgement.verdict()).isEqualTo(Verdict.WA);
        ResultDiff diff = judgement.diff();
        assertThat(diff.reason()).isEqualTo(MismatchReason.ROWS);
        assertThat(diff.expectedColumns()).containsExactly("id", "name");
        assertThat(diff.actualColumns()).containsExactly("id", "name");
        assertThat(diff.expectedRowCount()).isEqualTo(2);
        assertThat(diff.actualRowCount()).isEqualTo(3);
        assertThat(diff.missingRows()).containsExactly(List.of(number("2"), text("B")));
        assertThat(diff.extraRows())
                .containsExactly(List.of(number("3"), text("C")), List.of(number("4"), text("D")));
        assertThat(diff.orderMismatch()).isNull();
    }

    @Test
    @DisplayName("Lỗi SQL hay quá giờ ở testcase công khai thì không có diff")
    void judge_errorOnPublicTestcase_hasNoDiff() {
        assertThat(judge(run -> new ExecutionOutcome.TimedOut(2000), CODE, false).diff()).isNull();
        assertThat(
                        judge(
                                        run ->
                                                new ExecutionOutcome.Failed(
                                                        new SanitizedMessage("lỗi"), 3),
                                        CODE,
                                        false)
                                .diff())
                .isNull();
    }

    private static WireCell number(String value) {
        return new WireCell(CellKind.NUMBER, value);
    }

    private static WireCell text(String value) {
        return new WireCell(CellKind.TEXT, value);
    }

    @Test
    @DisplayName(
            "Lỗi SQL ở testcase công khai kèm thông báo; ở testcase ẩn thì không kèm thông báo")
    void judge_runtimeError_hidesMessageOnHiddenTestcase() {
        Sandbox failing =
                run -> new ExecutionOutcome.Failed(new SanitizedMessage("cột x không tồn tại"), 3);

        SubmissionJudgement onPublic = judge(failing, CODE, false);
        assertThat(onPublic.verdict()).isEqualTo(Verdict.RE);
        assertThat(onPublic.message()).isEqualTo("cột x không tồn tại");
        assertThat(onPublic.failedIndex()).isEqualTo(1);

        SubmissionJudgement onHidden =
                judge(
                        returning(
                                outcome(4, row(1, "A"), row(2, "B")),
                                new ExecutionOutcome.Failed(
                                        new SanitizedMessage("không ép được 'Bí mật' sang số"), 3)),
                        CODE,
                        false);
        assertThat(onHidden.verdict()).isEqualTo(Verdict.RE);
        assertThat(onHidden.failedIndex()).isEqualTo(2);
        assertThat(onHidden.failedIsHidden()).isTrue();
        assertThat(onHidden.message()).isNull();
    }

    @Test
    @DisplayName("Câu lệnh bị cấm thì FS ngay, không chạy sandbox và không báo tiến độ")
    void judge_forbiddenStatement_isRejectedBeforeRunning() {
        SubmissionJudgement judgement =
                judge(returning(outcome(4, row(1, "A"))), "DELETE FROM departments", false);

        assertThat(judgement.verdict()).isEqualTo(Verdict.FS);
        assertThat(judgement.message()).isEqualTo("Bài này chỉ cho phép SELECT, WITH");
        assertThat(judgement.failedIndex()).isNull();
        assertThat(judgement.judgedTestcases()).isZero();
        assertThat(runs).isEmpty();
        assertThat(progress).isEmpty();
    }

    @Test
    @DisplayName("Quá giờ thì TLE, lỗi sandbox hay không tải được dataset thì SE")
    void judge_timeoutAndInfrastructureFailures() {
        assertThat(judge(run -> new ExecutionOutcome.TimedOut(2000), CODE, false).verdict())
                .isEqualTo(Verdict.TLE);
        assertThat(
                        judge(run -> new ExecutionOutcome.SandboxFailed("lost", 1), CODE, false)
                                .verdict())
                .isEqualTo(Verdict.SE);

        SubmissionJudgeProcessor processor =
                new SubmissionJudgeProcessor(
                        new StatementGuard(),
                        (versionId, checksum) -> {
                            throw new SnapshotUnavailableException("problem-service down", null);
                        },
                        new SandboxRegistry(Map.of()));
        SubmissionJudgement unavailable = processor.judge(request(CODE, false), progress::add);
        assertThat(unavailable.verdict()).isEqualTo(Verdict.SE);
        assertThat(unavailable.judgedTestcases()).isZero();
    }

    private SubmissionJudgement judge(Sandbox sandbox, String code, boolean ordered) {
        runs.clear();
        progress.clear();
        Sandbox recording =
                run -> {
                    runs.add(run);
                    return sandbox.run(run);
                };
        SubmissionJudgeProcessor processor =
                new SubmissionJudgeProcessor(
                        new StatementGuard(),
                        (versionId, checksum) -> SNAPSHOT,
                        new SandboxRegistry(Map.of(Dialect.POSTGRESQL, recording)));
        return processor.judge(request(code, ordered), progress::add);
    }

    private static SubmissionJudgeRequest request(String code, boolean ordered) {
        JudgePackage judgePackage =
                new JudgePackage(
                        VERSION_ID,
                        CHECKSUM,
                        new CompareOptions(ordered, false, CompareOptions.DEFAULT_TOLERANCE),
                        2000,
                        Set.of(StatementType.SELECT, StatementType.WITH),
                        List.of(
                                new JudgeCase(1, SAMPLE_VARIANT, true, TWO_ROWS),
                                new JudgeCase(2, HIDDEN_VARIANT, false, TWO_ROWS),
                                new JudgeCase(3, OTHER_HIDDEN_VARIANT, false, TWO_ROWS)));
        return new SubmissionJudgeRequest(SUBMISSION_ID, 1, Dialect.POSTGRESQL, code, judgePackage);
    }

    private static Sandbox returning(ExecutionOutcome... outcomes) {
        List<ExecutionOutcome> queue = new ArrayList<>(Arrays.asList(outcomes));
        return run -> queue.remove(0);
    }

    @SafeVarargs
    private static ExecutionOutcome outcome(long runtimeMs, List<Object>... rows) {
        return new ExecutionOutcome.Succeeded(
                new ResultTable(List.of("id", "name"), List.of(rows)), runtimeMs);
    }

    private static List<Object> row(Object... values) {
        return Arrays.asList(values);
    }
}
