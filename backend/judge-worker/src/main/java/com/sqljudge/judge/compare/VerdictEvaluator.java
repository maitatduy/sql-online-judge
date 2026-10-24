package com.sqljudge.judge.compare;

import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import java.util.ArrayList;
import java.util.List;

public final class VerdictEvaluator {

    private final ResultNormalizer normalizer = new ResultNormalizer();
    private final ResultComparator comparator = new ResultComparator();

    public JudgeResult evaluate(
            List<JudgeTestcase> testcases, CompareOptions options, TestcaseExecutor executor) {
        if (testcases.isEmpty()) {
            throw new IllegalArgumentException("Problem has no testcases");
        }
        List<TestcaseOutcome> outcomes = new ArrayList<>();
        long maxRuntimeMs = 0;
        for (JudgeTestcase testcase : testcases) {
            ExecutionOutcome execution = executor.execute(testcase);
            maxRuntimeMs = Math.max(maxRuntimeMs, execution.runtimeMs());
            int number = testcase.number();
            if (!(execution instanceof Succeeded succeeded)) {
                Verdict verdict = failureVerdict(execution);
                outcomes.add(new TestcaseOutcome(number, verdict, execution.runtimeMs(), 0));
                String message = execution instanceof Failed failed ? failed.message() : null;
                return new JudgeResult(verdict, number, maxRuntimeMs, outcomes, null, message);
            }
            NormalizedTable actual = normalizer.normalize(succeeded.table());
            ComparisonResult comparison = comparator.compare(testcase.expected(), actual, options);
            Verdict verdict = comparison.verdict();
            outcomes.add(
                    new TestcaseOutcome(
                            number, verdict, succeeded.runtimeMs(), actual.rows().size()));
            if (!comparison.isAccepted()) {
                return new JudgeResult(verdict, number, maxRuntimeMs, outcomes, comparison, null);
            }
        }
        return new JudgeResult(Verdict.AC, null, maxRuntimeMs, outcomes, null, null);
    }

    private Verdict failureVerdict(ExecutionOutcome execution) {
        if (execution instanceof Failed) {
            return Verdict.RE;
        }
        if (execution instanceof TimedOut) {
            return Verdict.TLE;
        }
        return Verdict.SE;
    }
}
