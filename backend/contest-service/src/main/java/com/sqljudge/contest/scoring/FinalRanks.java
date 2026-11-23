package com.sqljudge.contest.scoring;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class FinalRanks {

    private FinalRanks() {}

    public static List<Ranked> rank(List<Result> results) {
        List<Result> ordered =
                results.stream()
                        .sorted(Comparator.comparingLong(Result::rankingScore).reversed())
                        .toList();
        List<Ranked> ranked = new ArrayList<>();
        for (int index = 0; index < ordered.size(); index++) {
            Result result = ordered.get(index);
            boolean tiesPrevious =
                    index > 0 && ordered.get(index - 1).rankingScore() == result.rankingScore();
            int rank = tiesPrevious ? ranked.get(index - 1).rank() : index + 1;
            ranked.add(new Ranked(result.userId(), rank, result.solved(), result.penaltyMinutes()));
        }
        return ranked;
    }

    public record Result(UUID userId, int solved, long penaltyMinutes) {

        long rankingScore() {
            return IcpcScoring.rankingScore(solved, penaltyMinutes);
        }
    }

    public record Ranked(UUID userId, int rank, int solved, long penaltyMinutes) {}
}
