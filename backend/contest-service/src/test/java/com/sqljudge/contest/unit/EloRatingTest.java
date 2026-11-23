package com.sqljudge.contest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.contest.scoring.EloRating;
import com.sqljudge.contest.scoring.EloRating.Change;
import com.sqljudge.contest.scoring.EloRating.Contestant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EloRatingTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CAROL = UUID.randomUUID();

    private static Map<UUID, Change> byUser(List<Change> changes) {
        return changes.stream().collect(Collectors.toMap(Change::userId, Function.identity()));
    }

    @Test
    @DisplayName("Hai người cùng 1500: người thắng được 16, người thua mất 16")
    void twoEqualPlayers_winnerGainsHalfK() {
        Map<UUID, Change> changes =
                byUser(
                        EloRating.apply(
                                List.of(
                                        new Contestant(ALICE, 1, 1500),
                                        new Contestant(BOB, 2, 1500))));

        assertThat(changes.get(ALICE).delta()).isEqualTo(16);
        assertThat(changes.get(ALICE).newRating()).isEqualTo(1516);
        assertThat(changes.get(BOB).delta()).isEqualTo(-16);
        assertThat(changes.get(BOB).oldRating()).isEqualTo(1500);
    }

    @Test
    @DisplayName("Thắng người mạnh hơn được cộng nhiều hơn, cùng hạng thì người yếu hơn được cộng")
    void upsetsAndTies_shiftTowardsTheWeaker() {
        Map<UUID, Change> upset =
                byUser(
                        EloRating.apply(
                                List.of(
                                        new Contestant(ALICE, 1, 1400),
                                        new Contestant(BOB, 2, 1800))));
        Map<UUID, Change> tie =
                byUser(
                        EloRating.apply(
                                List.of(
                                        new Contestant(ALICE, 1, 1400),
                                        new Contestant(BOB, 1, 1800))));

        assertThat(upset.get(ALICE).delta()).isGreaterThan(16);
        assertThat(tie.get(ALICE).delta()).isPositive();
        assertThat(tie.get(BOB).delta()).isNegative();
    }

    @Test
    @DisplayName("Nhiều người: hạng giữa của ba người ngang nhau không đổi rating")
    void middleOfThreeEqualPlayers_staysTheSame() {
        Map<UUID, Change> changes =
                byUser(
                        EloRating.apply(
                                List.of(
                                        new Contestant(ALICE, 1, 1500),
                                        new Contestant(BOB, 2, 1500),
                                        new Contestant(CAROL, 3, 1500))));

        assertThat(changes.get(ALICE).delta()).isEqualTo(16);
        assertThat(changes.get(BOB).delta()).isZero();
        assertThat(changes.get(CAROL).delta()).isEqualTo(-16);
    }

    @Test
    @DisplayName("Chỉ một người thi thì rating giữ nguyên")
    void singleContestant_keepsRating() {
        List<Change> changes = EloRating.apply(List.of(new Contestant(ALICE, 1, 1620)));

        assertThat(changes)
                .singleElement()
                .satisfies(
                        change -> {
                            assertThat(change.delta()).isZero();
                            assertThat(change.newRating()).isEqualTo(1620);
                        });
    }
}
