package com.sqljudge.judge.compare;

import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.SandboxFailed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import java.util.ArrayList;
import java.util.List;

public final class VerdictEvaluator {

    private static final SanitizedMessage UNSUPPORTED_COLUMN_TYPE =
            new SanitizedMessage("Kiểu dữ liệu của cột không được hỗ trợ");

    private final ResultNormalizer normalizer = new ResultNormalizer();
    private final ResultComparator comparator = new ResultComparator();

    public JudgeResult evaluate(
            List<JudgeTestcase> testcases, JudgeSettings settings, TestcaseExecutor executor) {
        if (testcases.isEmpty()) {
            throw new IllegalArgumentException("Problem has no testcases");
        }
        List<TestcaseOutcome> outcomes = new ArrayList<>();
        long maxRuntimeMs = 0;
        for (JudgeTestcase testcase : testcases) {
            boolean mayReveal = settings.mayReveal(testcase);
            ExecutionOutcome execution = executor.execute(testcase, mayReveal);
            maxRuntimeMs = Math.max(maxRuntimeMs, execution.runtimeMs());
            Judged judged = judge(testcase, execution, settings.compareOptions());
            outcomes.add(judged.outcome());
            if (judged.outcome().verdict() != Verdict.AC) {
                ComparisonResult revealedDiff = mayReveal ? judged.comparison() : null;
                return new JudgeResult(
                        judged.outcome().verdict(),
                        testcase.number(),
                        maxRuntimeMs,
                        outcomes,
                        revealedDiff,
                        judged.errorMessage(),
                        judged.systemErrorReason());
            }
        }
        return new JudgeResult(Verdict.AC, null, maxRuntimeMs, outcomes, null, null, null);
    }

    private Judged judge(
            JudgeTestcase testcase, ExecutionOutcome execution, CompareOptions options) {
        int number = testcase.number();
        long runtimeMs = execution.runtimeMs();
        if (execution instanceof Succeeded succeeded) {
            return compare(testcase, succeeded, options);
        }
        if (execution instanceof Failed failed) {
            return Judged.error(
                    new TestcaseOutcome(number, Verdict.RE, runtimeMs, 0), failed.message());
        }
        if (execution instanceof SandboxFailed failed) {
            return new Judged(
                    new TestcaseOutcome(number, Verdict.SE, runtimeMs, 0),
                    null,
                    null,
                    failed.reason());
        }
        return new Judged(new TestcaseOutcome(number, Verdict.TLE, runtimeMs, 0), null, null, null);
    }

    private Judged compare(JudgeTestcase testcase, Succeeded succeeded, CompareOptions options) {
        int number = testcase.number();
        NormalizedTable actual;
        try {
            actual = normalizer.normalize(succeeded.table());
        } catch (UnsupportedColumnTypeException exception) {
            return Judged.error(
                    new TestcaseOutcome(number, Verdict.RE, succeeded.runtimeMs(), 0),
                    UNSUPPORTED_COLUMN_TYPE);
        }
        ComparisonResult comparison = comparator.compare(testcase.expected(), actual, options);
        TestcaseOutcome outcome =
                new TestcaseOutcome(
                        number, comparison.verdict(), succeeded.runtimeMs(), actual.rows().size());
        return new Judged(outcome, comparison, null, null);
    }

    private record Judged(
            TestcaseOutcome outcome,
            ComparisonResult comparison,
            SanitizedMessage errorMessage,
            String systemErrorReason) {

        static Judged error(TestcaseOutcome outcome, SanitizedMessage errorMessage) {
            return new Judged(outcome, null, errorMessage, null);
        }
    }
}
