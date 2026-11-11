package com.sqljudge.submission.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.submission.entity.Dialect;
import com.sqljudge.submission.entity.JudgeOutcome;
import com.sqljudge.submission.entity.JudgeResult;
import com.sqljudge.submission.entity.Submission;
import com.sqljudge.submission.entity.Submission.SubmittedSolution;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.entity.Verdict;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubmissionTest {

    private static final Instant NOW = Instant.parse("2026-11-11T08:00:00Z");

    @Test
    @DisplayName(
            "Tiến độ chuyển bài sang đang chấm; tiến độ của lần chấm cũ hoặc sau khi xong bị bỏ"
                    + " qua")
    void recordProgress_updatesOnlyTheCurrentAttempt() {
        Submission submission = pending();

        assertThat(submission.recordProgress(1, 2)).isTrue();
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.JUDGING);
        assertThat(submission.getJudgedTestcases()).isEqualTo(2);
        assertThat(submission.recordProgress(1, 1)).isTrue();
        assertThat(submission.getJudgedTestcases()).isEqualTo(2);
        assertThat(submission.recordProgress(2, 3)).isFalse();

        submission.complete(result(1, Verdict.AC, null), NOW);
        assertThat(submission.recordProgress(1, 3)).isFalse();
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
    }

    @Test
    @DisplayName("Kết quả chấm được ghi đủ và chỉ ghi một lần")
    void complete_recordsVerdictOnce() {
        Submission submission = pending();

        JudgeOutcome outcome =
                submission.complete(new JudgeResult(1, Verdict.WA, 12, 2, true, null, 2), NOW);

        assertThat(outcome).isEqualTo(JudgeOutcome.COMPLETED);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
        assertThat(submission.getVerdict()).isEqualTo(Verdict.WA);
        assertThat(submission.getRuntimeMs()).isEqualTo(12);
        assertThat(submission.getFirstFailedIndex()).isEqualTo(2);
        assertThat(submission.getFirstFailedIsHidden()).isTrue();
        assertThat(submission.getJudgedTestcases()).isEqualTo(2);
        assertThat(submission.getJudgedAt()).isEqualTo(NOW);
        assertThat(submission.complete(result(1, Verdict.AC, null), NOW))
                .isEqualTo(JudgeOutcome.IGNORED);
        assertThat(submission.getVerdict()).isEqualTo(Verdict.WA);
    }

    @Test
    @DisplayName("Lỗi máy chấm được chấm lại tối đa 2 lần, lần thứ 3 vẫn lỗi thì ghi SE")
    void complete_systemError_retriesTwiceThenGivesUp() {
        Submission submission = pending();

        assertThat(submission.complete(result(1, Verdict.SE, null), NOW))
                .isEqualTo(JudgeOutcome.RETRY);
        assertThat(submission.getAttempts()).isEqualTo(2);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(submission.getJudgedTestcases()).isZero();
        assertThat(submission.complete(result(1, Verdict.SE, null), NOW))
                .isEqualTo(JudgeOutcome.IGNORED);
        assertThat(submission.complete(result(2, Verdict.SE, null), NOW))
                .isEqualTo(JudgeOutcome.RETRY);
        assertThat(submission.complete(result(3, Verdict.SE, null), NOW))
                .isEqualTo(JudgeOutcome.COMPLETED);
        assertThat(submission.getVerdict()).isEqualTo(Verdict.SE);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
    }

    private static JudgeResult result(int attempt, Verdict verdict, String message) {
        return new JudgeResult(attempt, verdict, 5, null, null, message, 3);
    }

    private static Submission pending() {
        return Submission.pending(
                UUID.randomUUID(),
                new SubmittedSolution(
                        UUID.randomUUID(), UUID.randomUUID(), Dialect.POSTGRESQL, "SELECT 1", 3),
                null);
    }
}
