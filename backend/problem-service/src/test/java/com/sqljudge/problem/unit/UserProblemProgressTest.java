package com.sqljudge.problem.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.problem.entity.ProblemProgressStatus;
import com.sqljudge.problem.entity.UserProblemProgress;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProblemProgressTest {

    private static final Instant FIRST_AC = Instant.parse("2026-11-12T08:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-12T09:00:00Z");

    @Test
    @DisplayName("Bài sai thì đã thử; AC đầu tiên chuyển sang đã giải và ghi thời điểm giải")
    void recordJudgement_firstAcceptedMarksSolvedOnce() {
        UserProblemProgress progress =
                UserProblemProgress.start(UUID.randomUUID(), UUID.randomUUID());

        assertThat(progress.recordJudgement(false, FIRST_AC)).isFalse();
        assertThat(progress.getStatus()).isEqualTo(ProblemProgressStatus.ATTEMPTED);
        assertThat(progress.getFirstSolvedAt()).isNull();

        assertThat(progress.recordJudgement(true, FIRST_AC)).isTrue();
        assertThat(progress.getStatus()).isEqualTo(ProblemProgressStatus.SOLVED);
        assertThat(progress.getFirstSolvedAt()).isEqualTo(FIRST_AC);
    }

    @Test
    @DisplayName("Đã giải rồi thì AC hay sai sau đó không đổi trạng thái hay thời điểm giải")
    void recordJudgement_afterSolved_keepsSolved() {
        UserProblemProgress progress =
                UserProblemProgress.start(UUID.randomUUID(), UUID.randomUUID());
        progress.recordJudgement(true, FIRST_AC);

        assertThat(progress.recordJudgement(true, LATER)).isFalse();
        assertThat(progress.recordJudgement(false, LATER)).isFalse();
        assertThat(progress.getStatus()).isEqualTo(ProblemProgressStatus.SOLVED);
        assertThat(progress.getFirstSolvedAt()).isEqualTo(FIRST_AC);
    }

    @Test
    @DisplayName("AC đến muộn nhưng chấm sớm hơn thì thời điểm giải lùi về lần AC sớm nhất")
    void recordJudgement_earlierAcceptedArrivingLate_movesSolvedAtBack() {
        UserProblemProgress progress =
                UserProblemProgress.start(UUID.randomUUID(), UUID.randomUUID());
        progress.recordJudgement(true, LATER);

        assertThat(progress.recordJudgement(true, FIRST_AC)).isFalse();
        assertThat(progress.getFirstSolvedAt()).isEqualTo(FIRST_AC);
    }
}
