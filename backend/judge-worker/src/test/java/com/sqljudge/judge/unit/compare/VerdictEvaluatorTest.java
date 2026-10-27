package com.sqljudge.judge.unit.compare;

import static com.sqljudge.judge.unit.compare.Tables.columns;
import static com.sqljudge.judge.unit.compare.Tables.row;
import static com.sqljudge.judge.unit.compare.Tables.table;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.judge.compare.CompareOptions;
import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.SandboxFailed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import com.sqljudge.judge.compare.JudgeResult;
import com.sqljudge.judge.compare.JudgeSettings;
import com.sqljudge.judge.compare.JudgeTestcase;
import com.sqljudge.judge.compare.MismatchReason;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.compare.SanitizedMessage;
import com.sqljudge.judge.compare.TestcaseOutcome;
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.compare.VerdictEvaluator;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerdictEvaluatorTest {

    private static final CompareOptions ORDERED =
            new CompareOptions(true, false, CompareOptions.DEFAULT_TOLERANCE);
    private static final JudgeSettings PRACTICE = new JudgeSettings(ORDERED, true);
    private static final JudgeSettings CONTEST_WITHOUT_DIFF = new JudgeSettings(ORDERED, false);

    private final VerdictEvaluator evaluator = new VerdictEvaluator();
    private final List<Integer> executed = new ArrayList<>();
    private final List<Boolean> revealFlags = new ArrayList<>();

    private static JudgeTestcase publicTestcase(int number, int value) {
        return new JudgeTestcase(number, false, table(columns("v"), row(value)));
    }

    private static JudgeTestcase hiddenTestcase(int number, int value) {
        return new JudgeTestcase(number, true, table(columns("v"), row(value)));
    }

    private static Succeeded rowsOf(long runtimeMs, Object... values) {
        List<List<Object>> rows = new ArrayList<>();
        for (Object value : values) {
            rows.add(row(value));
        }
        return new Succeeded(new ResultTable(columns("v"), rows), runtimeMs);
    }

    private JudgeResult run(
            List<JudgeTestcase> testcases,
            JudgeSettings settings,
            Map<Integer, ExecutionOutcome> outcomes) {
        return evaluator.evaluate(
                testcases,
                settings,
                (testcase, revealToUser) -> {
                    executed.add(testcase.number());
                    revealFlags.add(revealToUser);
                    return outcomes.get(testcase.number());
                });
    }

    @Test
    @DisplayName("Executor chỉ được phép lộ thông báo lỗi khi testcase công khai và đề cho lộ")
    void evaluate_passesRevealFlagFromSingleSource() {
        Map<Integer, ExecutionOutcome> allPass = Map.of(1, rowsOf(1, 10), 2, rowsOf(1, 20));
        List<JudgeTestcase> mixed = List.of(publicTestcase(1, 10), hiddenTestcase(2, 20));

        run(mixed, PRACTICE, allPass);
        run(mixed, CONTEST_WITHOUT_DIFF, allPass);

        assertThat(revealFlags).containsExactly(true, false, false, false);
    }

    @Test
    @DisplayName("Mọi testcase đúng -> AC, ghi thời gian lớn nhất và số dòng từng testcase")
    void evaluate_allTestcasesPass_returnsAccepted() {
        JudgeResult result =
                run(
                        List.of(publicTestcase(1, 10), hiddenTestcase(2, 20)),
                        PRACTICE,
                        Map.of(1, rowsOf(30, 10), 2, rowsOf(45, 20)));

        assertThat(result.verdict()).isEqualTo(Verdict.AC);
        assertThat(result.firstFailedCase()).isNull();
        assertThat(result.maxRuntimeMs()).isEqualTo(45);
        assertThat(result.revealedDiff()).isNull();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.systemErrorReason()).isNull();
        assertThat(result.testcases())
                .containsExactly(
                        new TestcaseOutcome(1, Verdict.AC, 30, 1),
                        new TestcaseOutcome(2, Verdict.AC, 45, 1));
    }

    @Test
    @DisplayName("B14 · Dừng ở testcase sai đầu tiên, testcase công khai thì có diff")
    void evaluate_stopsAtFirstFailedPublicTestcase_withDiff() {
        JudgeResult result =
                run(
                        List.of(
                                publicTestcase(1, 10),
                                publicTestcase(2, 20),
                                publicTestcase(3, 30)),
                        PRACTICE,
                        Map.of(1, rowsOf(5, 10), 2, rowsOf(7, 99), 3, rowsOf(9, 30)));

        assertThat(result.verdict()).isEqualTo(Verdict.WA);
        assertThat(result.firstFailedCase()).isEqualTo(2);
        assertThat(executed).containsExactly(1, 2);
        assertThat(result.maxRuntimeMs()).isEqualTo(7);
        assertThat(result.revealedDiff().reason()).isEqualTo(MismatchReason.ROWS);
        assertThat(result.revealedDiff().missingRows()).containsExactly(0);
        assertThat(result.revealedDiff().extraRows()).containsExactly(0);
        assertThat(result.testcases())
                .extracting(TestcaseOutcome::verdict)
                .containsExactly(Verdict.AC, Verdict.WA);
    }

    @Test
    @DisplayName("Testcase ẩn sai -> WA nhưng không có diff")
    void evaluate_hiddenTestcaseFails_hidesDiff() {
        JudgeResult result =
                run(List.of(hiddenTestcase(1, 10)), PRACTICE, Map.of(1, rowsOf(5, 99)));

        assertThat(result.verdict()).isEqualTo(Verdict.WA);
        assertThat(result.firstFailedCase()).isEqualTo(1);
        assertThat(result.revealedDiff()).isNull();
    }

    @Test
    @DisplayName("Contest tắt diff -> testcase công khai sai cũng không có diff")
    void evaluate_diffDisabled_hidesDiffOfPublicTestcase() {
        JudgeResult result =
                run(List.of(publicTestcase(1, 10)), CONTEST_WITHOUT_DIFF, Map.of(1, rowsOf(5, 99)));

        assertThat(result.verdict()).isEqualTo(Verdict.WA);
        assertThat(result.revealedDiff()).isNull();
    }

    @Test
    @DisplayName("Sai thứ tự ở testcase công khai -> OM kèm dòng lệch đầu tiên")
    void evaluate_orderMismatch_returnsOrderMismatch() {
        JudgeTestcase ordered = new JudgeTestcase(1, false, table(columns("v"), row(1), row(2)));

        JudgeResult result = run(List.of(ordered), PRACTICE, Map.of(1, rowsOf(3, 2, 1)));

        assertThat(result.verdict()).isEqualTo(Verdict.OM);
        assertThat(result.firstFailedCase()).isEqualTo(1);
        assertThat(result.revealedDiff().firstOrderMismatchRow()).isZero();
        assertThat(result.testcases()).containsExactly(new TestcaseOutcome(1, Verdict.OM, 3, 2));
    }

    @Test
    @DisplayName("Lỗi khi chạy -> RE kèm thông báo đã lọc, không so khớp")
    void evaluate_executionError_returnsRuntimeError() {
        SanitizedMessage message = new SanitizedMessage("column \"x\" does not exist");

        JudgeResult result =
                run(
                        List.of(publicTestcase(1, 10), publicTestcase(2, 20)),
                        PRACTICE,
                        Map.of(1, new Failed(message, 4)));

        assertThat(result.verdict()).isEqualTo(Verdict.RE);
        assertThat(result.firstFailedCase()).isEqualTo(1);
        assertThat(result.errorMessage()).isEqualTo(message);
        assertThat(result.revealedDiff()).isNull();
        assertThat(executed).containsExactly(1);
        assertThat(result.testcases()).containsExactly(new TestcaseOutcome(1, Verdict.RE, 4, 0));
    }

    @Test
    @DisplayName("Cột có kiểu không hỗ trợ -> RE với thông báo tiếng Việt, không ném lỗi")
    void evaluate_unsupportedColumnType_returnsRuntimeError() {
        JudgeResult result =
                run(
                        List.of(publicTestcase(1, 10), publicTestcase(2, 20)),
                        PRACTICE,
                        Map.of(1, rowsOf(6, Timestamp.from(Instant.EPOCH))));

        assertThat(result.verdict()).isEqualTo(Verdict.RE);
        assertThat(result.firstFailedCase()).isEqualTo(1);
        assertThat(result.errorMessage())
                .isEqualTo(new SanitizedMessage("Kiểu dữ liệu của cột không được hỗ trợ"));
        assertThat(result.systemErrorReason()).isNull();
        assertThat(executed).containsExactly(1);
        assertThat(result.testcases()).containsExactly(new TestcaseOutcome(1, Verdict.RE, 6, 0));
    }

    @Test
    @DisplayName("Hết thời gian -> TLE")
    void evaluate_timeout_returnsTimeLimitExceeded() {
        JudgeResult result =
                run(
                        List.of(publicTestcase(1, 10), publicTestcase(2, 20)),
                        PRACTICE,
                        Map.of(1, rowsOf(5, 10), 2, new TimedOut(2000)));

        assertThat(result.verdict()).isEqualTo(Verdict.TLE);
        assertThat(result.firstFailedCase()).isEqualTo(2);
        assertThat(result.maxRuntimeMs()).isEqualTo(2000);
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("Sandbox lỗi -> SE, lý do chỉ nằm ở trường dành cho admin")
    void evaluate_sandboxFailure_returnsSystemError() {
        JudgeResult result =
                run(
                        List.of(publicTestcase(1, 10)),
                        PRACTICE,
                        Map.of(1, new SandboxFailed("connection refused", 0)));

        assertThat(result.verdict()).isEqualTo(Verdict.SE);
        assertThat(result.firstFailedCase()).isEqualTo(1);
        assertThat(result.errorMessage()).isNull();
        assertThat(result.systemErrorReason()).isEqualTo("connection refused");
    }

    @Test
    @DisplayName("Không có testcase nào -> báo lỗi cấu hình, không cho AC miễn phí")
    void evaluate_noTestcases_throws() {
        assertThatThrownBy(() -> run(List.of(), PRACTICE, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(executed).isEmpty();
    }
}
