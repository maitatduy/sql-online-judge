package com.sqljudge.judge.job;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.DatasetSnapshotResponse.SnapshotVariant;
import com.sqljudge.judge.client.SnapshotMapper;
import com.sqljudge.judge.client.SnapshotSource;
import com.sqljudge.judge.client.SnapshotUnavailableException;
import com.sqljudge.judge.compare.ComparisonResult;
import com.sqljudge.judge.compare.ExecutionOutcome;
import com.sqljudge.judge.compare.ExecutionOutcome.Failed;
import com.sqljudge.judge.compare.ExecutionOutcome.Succeeded;
import com.sqljudge.judge.compare.ExecutionOutcome.TimedOut;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.ResultComparator;
import com.sqljudge.judge.compare.ResultNormalizer;
import com.sqljudge.judge.compare.UnsupportedColumnTypeException;
import com.sqljudge.judge.compare.Verdict;
import com.sqljudge.judge.guard.GuardDecision;
import com.sqljudge.judge.guard.GuardPolicy;
import com.sqljudge.judge.guard.StatementGuard;
import com.sqljudge.judge.job.JudgePackage.JudgeCase;
import com.sqljudge.judge.sandbox.QueryRun;
import com.sqljudge.judge.sandbox.RunLimits;
import com.sqljudge.judge.sandbox.Sandbox;
import com.sqljudge.judge.sandbox.SandboxRegistry;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class SubmissionJudgeProcessor {

    private static final String UNSUPPORTED_COLUMN_TYPE = "Kiểu dữ liệu của cột không được hỗ trợ";

    private final StatementGuard guard;
    private final SnapshotSource snapshots;
    private final SandboxRegistry sandboxes;
    private final ResultNormalizer normalizer = new ResultNormalizer();
    private final ResultComparator comparator = new ResultComparator();

    public SubmissionJudgeProcessor(
            StatementGuard guard, SnapshotSource snapshots, SandboxRegistry sandboxes) {
        this.guard = guard;
        this.snapshots = snapshots;
        this.sandboxes = sandboxes;
    }

    public SubmissionJudgement judge(
            SubmissionJudgeRequest request, Consumer<JudgeProgress> progress) {
        JudgePackage judgePackage = request.judgePackage();
        if (guard.check(request.code(), policyOf(judgePackage))
                instanceof GuardDecision.Rejected rejected) {
            return Tally.start(request).rejected(rejected.verdict(), rejected.message().text());
        }
        Optional<Sandbox> sandbox = sandboxes.forDialect(request.dialect());
        DatasetSnapshotResponse snapshot;
        try {
            snapshot =
                    snapshots.snapshotOf(
                            judgePackage.datasetVersionId(), judgePackage.datasetChecksum());
        } catch (SnapshotUnavailableException exception) {
            return Tally.start(request).rejected(Verdict.SE, null);
        }
        if (sandbox.isEmpty()) {
            return Tally.start(request).rejected(Verdict.SE, null);
        }
        Tally tally = Tally.start(request);
        RunLimits limits = RunLimits.forTimeLimit(judgePackage.timeLimitMs());
        for (JudgeCase testcase : judgePackage.testcases()) {
            Optional<SnapshotVariant> variant = variantOf(snapshot, testcase);
            if (variant.isEmpty()) {
                return tally.failed(testcase, Verdict.SE, null);
            }
            ExecutionOutcome outcome =
                    sandbox.get()
                            .run(
                                    new QueryRun(
                                            SnapshotMapper.toDataset(snapshot, variant.get()),
                                            request.code(),
                                            limits));
            tally = tally.judged(outcome.runtimeMs());
            progress.accept(tally.progress());
            Optional<SubmissionJudgement> failure =
                    failureOf(tally, testcase, outcome, judgePackage);
            if (failure.isPresent()) {
                return failure.get();
            }
        }
        return tally.accepted();
    }

    private Optional<SubmissionJudgement> failureOf(
            Tally tally, JudgeCase testcase, ExecutionOutcome outcome, JudgePackage judgePackage) {
        if (outcome instanceof Failed failed) {
            String message = testcase.isPublic() ? failed.message().text() : null;
            return Optional.of(tally.failed(testcase, Verdict.RE, message));
        }
        if (outcome instanceof TimedOut) {
            return Optional.of(tally.failed(testcase, Verdict.TLE, null));
        }
        if (!(outcome instanceof Succeeded succeeded)) {
            return Optional.of(tally.failed(testcase, Verdict.SE, null));
        }
        NormalizedTable actual;
        try {
            actual = normalizer.normalize(succeeded.table());
        } catch (UnsupportedColumnTypeException exception) {
            return Optional.of(tally.failed(testcase, Verdict.RE, UNSUPPORTED_COLUMN_TYPE));
        }
        ComparisonResult comparison =
                comparator.compare(testcase.expected(), actual, judgePackage.compareOptions());
        if (comparison.isAccepted()) {
            return Optional.empty();
        }
        ResultDiff diff =
                testcase.isPublic() ? ResultDiff.of(testcase.expected(), actual, comparison) : null;
        return Optional.of(tally.mismatched(testcase, comparison.verdict(), diff));
    }

    private static GuardPolicy policyOf(JudgePackage judgePackage) {
        return judgePackage.allowedStatements().isEmpty()
                ? GuardPolicy.selectOnly()
                : GuardPolicy.of(judgePackage.allowedStatements());
    }

    private static Optional<SnapshotVariant> variantOf(
            DatasetSnapshotResponse snapshot, JudgeCase testcase) {
        return snapshot.variants().stream()
                .filter(variant -> variant.id().equals(testcase.variantId()))
                .findFirst();
    }

    private record Tally(SubmissionJudgeRequest request, int judged, long runtimeMs) {

        static Tally start(SubmissionJudgeRequest request) {
            return new Tally(request, 0, 0);
        }

        Tally judged(long testcaseRuntimeMs) {
            return new Tally(request, judged + 1, Math.max(runtimeMs, testcaseRuntimeMs));
        }

        JudgeProgress progress() {
            return new JudgeProgress(request.submissionId(), request.attempt(), judged, total());
        }

        SubmissionJudgement accepted() {
            return judgement(Verdict.AC, null, null, null, null);
        }

        SubmissionJudgement rejected(Verdict verdict, String message) {
            return judgement(verdict, null, null, message, null);
        }

        SubmissionJudgement failed(JudgeCase testcase, Verdict verdict, String message) {
            return judgement(verdict, testcase.index(), !testcase.isPublic(), message, null);
        }

        SubmissionJudgement mismatched(JudgeCase testcase, Verdict verdict, ResultDiff diff) {
            return judgement(verdict, testcase.index(), !testcase.isPublic(), null, diff);
        }

        private SubmissionJudgement judgement(
                Verdict verdict,
                Integer failedIndex,
                Boolean failedIsHidden,
                String message,
                ResultDiff diff) {
            return new SubmissionJudgement(
                    request.submissionId(),
                    request.attempt(),
                    verdict,
                    runtimeMs,
                    failedIndex,
                    failedIsHidden,
                    message,
                    diff,
                    judged,
                    total());
        }

        private int total() {
            return request.judgePackage().testcases().size();
        }
    }
}
