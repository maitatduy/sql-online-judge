package com.sqljudge.judge.unit.compare;

import static com.sqljudge.judge.unit.compare.Tables.cells;
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
import com.sqljudge.judge.compare.JudgeTestcase;
import com.sqljudge.judge.compare.MismatchReason;
import com.sqljudge.judge.compare.ResultTable;
import com.sqljudge.judge.compare.TestcaseOutcome;
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.compare.VerdictEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerdictEvaluatorTest {

    private static final CompareOptions OPTIONS = CompareOptions.defaults().withOrdered(true);

    private final VerdictEvaluator evaluator = new VerdictEvaluator();
    private final List<Integer> executed = new ArrayList<>();

    private static JudgeTestcase testcase(int number, int value) {
        return new JudgeTestcase(number, table(columns("v"), row(value)));
    }

    private static Succeeded rowsOf(long runtimeMs, int... values) {
        List<List<Object>> rows = new ArrayList<>();
        for (int value : values) {
            rows.add(row(value));
        }
        return new Succeeded(new ResultTable(columns("v"), rows), runtimeMs);
    }

    private JudgeResult run(
            List<JudgeTestcase> testcases, Map<Integer, ExecutionOutcome> outcomes) {
        return evaluator.evaluate(
                testcases,
                OPTIONS,
                testcase -> {
                    executed.add(testcase.number());
                    return outcomes.get(testcase.number());
                });
    }

    @Test
    @DisplayName("Mọi testcase đúng -> AC, ghi thời gian lớn nhất và số dòng từng testcase")
    void evaluate_allTestcasesPass_returnsAccepted() {
        JudgeResult result =
                run(
                        List.of(testcase(1, 10), testcase(2, 20)),
                        Map.of(1, rowsOf(30, 10), 2, rowsOf(45, 20)));

        assertThat(result.verdict()).isEqualTo(Verdict.AC);
        assertThat(result.firstFailedCase()).isNull();
        assertThat(result.maxRuntimeMs()).isEqualTo(45);
        assertThat(result.failedComparison()).isNull();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.testcases())
                .containsExactly(
                        new TestcaseOutcome(1, Verdict.AC, 30, 1),
                        new TestcaseOutcome(2, Verdict.AC, 45, 1));
    }

    @Test
    @DisplayName("B14 · Dừng ở testcase sai đầu tiên, không chạy testcase sau")
    void evaluate_stopsAtFirstFailedTestcase() {
        JudgeResult result =
                run(
                        List.of(testcase(1, 10), testcase(2, 20), testcase(3, 30)),
                        Map.of(1, rowsOf(5, 10), 2, rowsOf(7, 99), 3, rowsOf(9, 30)));

        assertThat(result.verdict()).isEqualTo(Verdict.WA);
        assertThat(result.firstFailedCase()).isEqualTo(2);
        assertThat(executed).containsExactly(1, 2);
        assertThat(result.maxRuntimeMs()).isEqualTo(7);
        assertThat(result.failedComparison().reason()).isEqualTo(MismatchReason.ROWS);
        assertThat(result.failedComparison().missingRows()).containsExactly(cells(20));
        assertThat(result.failedComparison().extraRows()).containsExactly(cells(99));
        assertThat(result.testcases())
                .extracting(TestcaseOutcome::verdict)
                .containsExactly(Verdict.AC, Verdict.WA);
    }

    @Test
    @DisplayName("Sai thứ tự ở testcase đầu -> OM")
    void evaluate_orderMismatch_returnsOrderMismatch() {
        JudgeTestcase ordered = new JudgeTestcase(1, table(columns("v"), row(1), row(2)));

        JudgeResult result = run(List.of(ordered), Map.of(1, rowsOf(3, 2, 1)));

        assertThat(result.verdict()).isEqualTo(Verdict.OM);
        assertThat(result.firstFailedCase()).isEqualTo(1);
        assertThat(result.testcases()).containsExactly(new TestcaseOutcome(1, Verdict.OM, 3, 2));
    }

    @Test
    @DisplayName("Lỗi khi chạy -> RE kèm thông báo, không so khớp")
    void evaluate_executionError_returnsRuntimeError() {
        JudgeResult result =
                run(
                        List.of(testcase(1, 10), testcase(2, 20)),
                        Map.of(1, new Failed("column \"x\" does not exist", 4)));

        assertThat(result.verdict()).isEqualTo(Verdict.RE);
        assertThat(result.firstFailedCase()).isEqualTo(1);
        assertThat(result.errorMessage()).isEqualTo("column \"x\" does not exist");
        assertThat(result.failedComparison()).isNull();
        assertThat(executed).containsExactly(1);
        assertThat(result.testcases()).containsExactly(new TestcaseOutcome(1, Verdict.RE, 4, 0));
    }

    @Test
    @DisplayName("Hết thời gian -> TLE")
    void evaluate_timeout_returnsTimeLimitExceeded() {
        JudgeResult result =
                run(
                        List.of(testcase(1, 10), testcase(2, 20)),
                        Map.of(1, rowsOf(5, 10), 2, new TimedOut(2000)));

        assertThat(result.verdict()).isEqualTo(Verdict.TLE);
        assertThat(result.firstFailedCase()).isEqualTo(2);
        assertThat(result.maxRuntimeMs()).isEqualTo(2000);
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("Sandbox lỗi -> SE, không lộ lý do nội bộ ra errorMessage")
    void evaluate_sandboxFailure_returnsSystemError() {
        JudgeResult result =
                run(
                        List.of(testcase(1, 10)),
                        Map.of(1, new SandboxFailed("connection refused", 0)));

        assertThat(result.verdict()).isEqualTo(Verdict.SE);
        assertThat(result.firstFailedCase()).isEqualTo(1);
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("Không có testcase nào -> báo lỗi cấu hình, không cho AC miễn phí")
    void evaluate_noTestcases_throws() {
        assertThatThrownBy(() -> run(List.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(executed).isEmpty();
    }
}
