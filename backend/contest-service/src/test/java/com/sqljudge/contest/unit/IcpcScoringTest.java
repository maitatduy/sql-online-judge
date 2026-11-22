package com.sqljudge.contest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.contest.scoring.Attempt;
import com.sqljudge.contest.scoring.IcpcScoring;
import com.sqljudge.contest.scoring.JudgeVerdict;
import com.sqljudge.contest.scoring.ProblemScore;
import com.sqljudge.contest.scoring.ScoringRules;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IcpcScoringTest {

    private static final Instant START = Instant.parse("2026-11-30T12:00:00Z");
    private static final ScoringRules RULES =
            new ScoringRules(START, START.plus(Duration.ofMinutes(60)), 20);

    private static Attempt at(int minute, int second, JudgeVerdict verdict) {
        return new Attempt(START.plus(Duration.ofMinutes(minute).plusSeconds(second)), verdict);
    }

    @Test
    @DisplayName("AC ngay lần đầu thì phạt bằng số phút từ lúc bắt đầu, làm tròn xuống")
    void firstTryAccepted_penaltyIsElapsedMinutes() {
        ProblemScore score = IcpcScoring.score(List.of(at(13, 59, JudgeVerdict.AC)), RULES);

        assertThat(score.isSolved()).isTrue();
        assertThat(score.acceptedMinute()).isEqualTo(13);
        assertThat(score.wrongAttempts()).isZero();
        assertThat(score.penaltyMinutes()).isEqualTo(13);
    }

    @Test
    @DisplayName("Mỗi lần sai trước AC cộng thêm phút phạt, RE, FS và SE không tính")
    void wrongAttemptsBeforeAccepted_addPenalty() {
        ProblemScore score =
                IcpcScoring.score(
                        List.of(
                                at(5, 0, JudgeVerdict.WA),
                                at(6, 0, JudgeVerdict.RE),
                                at(7, 0, JudgeVerdict.FS),
                                at(8, 0, JudgeVerdict.SE),
                                at(9, 0, JudgeVerdict.TLE),
                                at(10, 0, JudgeVerdict.OM),
                                at(30, 0, JudgeVerdict.AC)),
                        RULES);

        assertThat(score.wrongAttempts()).isEqualTo(3);
        assertThat(score.penaltyMinutes()).isEqualTo(30 + 3 * 20);
    }

    @Test
    @DisplayName("Xét theo giờ nộp chứ không theo thứ tự chấm, lượt nộp sau AC bị bỏ qua")
    void attemptsAreOrderedBySubmissionTime() {
        ProblemScore score =
                IcpcScoring.score(
                        List.of(
                                at(40, 0, JudgeVerdict.WA),
                                at(20, 0, JudgeVerdict.AC),
                                at(10, 0, JudgeVerdict.WA)),
                        RULES);

        assertThat(score.acceptedMinute()).isEqualTo(20);
        assertThat(score.wrongAttempts()).isEqualTo(1);
        assertThat(score.penaltyMinutes()).isEqualTo(40);
    }

    @Test
    @DisplayName("Chưa AC thì không bị phạt nhưng vẫn đếm số lần sai")
    void unsolvedProblem_hasNoPenalty() {
        ProblemScore score =
                IcpcScoring.score(
                        List.of(at(5, 0, JudgeVerdict.WA), at(6, 0, JudgeVerdict.WA)), RULES);

        assertThat(score.isSolved()).isFalse();
        assertThat(score.acceptedMinute()).isNull();
        assertThat(score.wrongAttempts()).isEqualTo(2);
        assertThat(score.penaltyMinutes()).isZero();
        assertThat(score.pendingAttempts()).isZero();
    }

    @Test
    @DisplayName("Bảng công khai chỉ tính lượt nộp trước đóng băng, lượt sau hiện là đang chờ")
    void publicScore_hidesAttemptsAfterFreeze() {
        List<Attempt> attempts =
                List.of(
                        at(50, 0, JudgeVerdict.WA),
                        at(61, 0, JudgeVerdict.RE),
                        at(62, 0, JudgeVerdict.WA),
                        at(70, 0, JudgeVerdict.AC));

        ProblemScore actual = IcpcScoring.score(attempts, RULES);
        ProblemScore shown = IcpcScoring.publicScore(attempts, RULES);

        assertThat(actual.isSolved()).isTrue();
        assertThat(actual.penaltyMinutes()).isEqualTo(70 + 2 * 20);
        assertThat(shown.isSolved()).isFalse();
        assertThat(shown.wrongAttempts()).isEqualTo(1);
        assertThat(shown.pendingAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("Đã AC trước đóng băng thì bảng công khai giống bảng thật, không có lượt chờ")
    void publicScore_solvedBeforeFreeze_showsResult() {
        List<Attempt> attempts = List.of(at(15, 0, JudgeVerdict.AC), at(65, 0, JudgeVerdict.WA));

        ProblemScore shown = IcpcScoring.publicScore(attempts, RULES);

        assertThat(shown).isEqualTo(IcpcScoring.score(attempts, RULES));
        assertThat(shown.pendingAttempts()).isZero();
    }

    @Test
    @DisplayName("Điểm xếp hạng đặt số bài giải lên trước, tổng phạt nhỏ hơn thì đứng trên")
    void rankingScore_ordersBySolvedThenPenalty() {
        assertThat(IcpcScoring.rankingScore(2, 300)).isGreaterThan(IcpcScoring.rankingScore(1, 0));
        assertThat(IcpcScoring.rankingScore(2, 100))
                .isGreaterThan(IcpcScoring.rankingScore(2, 101));
        assertThat(IcpcScoring.rankingScore(3, 45)).isEqualTo(3 * 10_000_000L - 45);
    }
}
