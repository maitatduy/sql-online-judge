package com.sqljudge.contest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.contest.standings.StandingRanks;
import com.sqljudge.contest.standings.StandingRow;
import com.sqljudge.contest.standings.StandingsStore.RankedRow;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StandingRanksTest {

    @Test
    @DisplayName("Xếp theo số bài rồi phạt, bằng điểm thì cùng hạng, hạng sau bỏ qua chỗ trống")
    void rank_sortsBySolvedThenPenaltyWithSharedRanks() {
        StandingRow alice = row("alice", 2, 70);
        StandingRow bob = row("bob", 2, 20);
        StandingRow carol = row("carol", 2, 20);
        StandingRow erin = row("erin", 0, 0);

        List<RankedRow> ranked = StandingRanks.rank(List.of(erin, alice, bob, carol));

        assertThat(ranked).extracting(RankedRow::rank).containsExactly(1, 1, 3, 4);
        assertThat(ranked.get(2).row()).isEqualTo(alice);
        assertThat(ranked.get(3).row()).isEqualTo(erin);
        assertThat(ranked.subList(0, 2))
                .extracting(rankedRow -> rankedRow.row().username())
                .containsExactlyInAnyOrder("bob", "carol");
    }

    @Test
    @DisplayName("Không có ai thì danh sách rỗng")
    void rank_noRows_isEmpty() {
        assertThat(StandingRanks.rank(List.of())).isEmpty();
    }

    private static StandingRow row(String username, int solved, long penaltyMinutes) {
        return new StandingRow(
                UUID.randomUUID(), username, username, solved, penaltyMinutes, List.of());
    }
}
