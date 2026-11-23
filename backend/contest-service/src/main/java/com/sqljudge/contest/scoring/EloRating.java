package com.sqljudge.contest.scoring;

import java.util.List;
import java.util.UUID;

public final class EloRating {

    public static final int INITIAL_RATING = 1500;
    private static final double K_FACTOR = 32;
    private static final double SCALE = 400;
    private static final double WIN = 1;
    private static final double TIE = 0.5;
    private static final double LOSS = 0;

    private EloRating() {}

    public static List<Change> apply(List<Contestant> contestants) {
        int opponents = contestants.size() - 1;
        return contestants.stream()
                .map(
                        contestant -> {
                            int delta =
                                    opponents == 0
                                            ? 0
                                            : (int)
                                                    Math.round(
                                                            K_FACTOR
                                                                    * surplusOf(
                                                                            contestant, contestants)
                                                                    / opponents);
                            return new Change(
                                    contestant.userId(),
                                    contestant.rank(),
                                    contestant.rating(),
                                    contestant.rating() + delta,
                                    delta);
                        })
                .toList();
    }

    private static double surplusOf(Contestant contestant, List<Contestant> contestants) {
        return contestants.stream()
                .filter(opponent -> !opponent.userId().equals(contestant.userId()))
                .mapToDouble(
                        opponent ->
                                actualScore(contestant, opponent)
                                        - expectedScore(contestant, opponent))
                .sum();
    }

    private static double expectedScore(Contestant contestant, Contestant opponent) {
        return 1 / (1 + Math.pow(10, (opponent.rating() - contestant.rating()) / SCALE));
    }

    private static double actualScore(Contestant contestant, Contestant opponent) {
        if (contestant.rank() == opponent.rank()) {
            return TIE;
        }
        return contestant.rank() < opponent.rank() ? WIN : LOSS;
    }

    public record Contestant(UUID userId, int rank, int rating) {}

    public record Change(UUID userId, int rank, int oldRating, int newRating, int delta) {}
}
