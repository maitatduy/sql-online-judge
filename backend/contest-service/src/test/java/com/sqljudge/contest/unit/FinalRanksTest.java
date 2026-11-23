package com.sqljudge.contest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.contest.scoring.FinalRanks;
import com.sqljudge.contest.scoring.FinalRanks.Ranked;
import com.sqljudge.contest.scoring.FinalRanks.Result;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FinalRanksTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CAROL = UUID.randomUUID();
    private static final UUID DAVE = UUID.randomUUID();

    @Test
    @DisplayName("Xếp theo số bài rồi tổng phạt, bằng điểm thì cùng hạng và hạng sau nhảy cóc")
    void rank_ordersBySolvedThenPenaltyWithTies() {
        List<Ranked> ranked =
                FinalRanks.rank(
                        List.of(
                                new Result(CAROL, 1, 10),
                                new Result(ALICE, 2, 70),
                                new Result(BOB, 2, 20),
                                new Result(DAVE, 2, 20)));

        assertThat(ranked)
                .extracting(Ranked::userId)
                .containsExactlyInAnyOrder(BOB, DAVE, ALICE, CAROL);
        assertThat(ranked).extracting(Ranked::rank).containsExactly(1, 1, 3, 4);
        assertThat(ranked.get(2).userId()).isEqualTo(ALICE);
        assertThat(ranked.get(3).userId()).isEqualTo(CAROL);
    }

    @Test
    @DisplayName("Không ai nộp bài thì không có hạng nào")
    void rank_emptyContest_isEmpty() {
        assertThat(FinalRanks.rank(List.of())).isEmpty();
    }
}
