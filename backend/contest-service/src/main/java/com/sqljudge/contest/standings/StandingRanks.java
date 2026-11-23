package com.sqljudge.contest.standings;

import com.sqljudge.contest.scoring.IcpcScoring;
import com.sqljudge.contest.standings.StandingsStore.RankedRow;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StandingRanks {

    private StandingRanks() {}

    public static List<RankedRow> rank(List<StandingRow> rows) {
        List<StandingRow> sorted =
                rows.stream()
                        .sorted(Comparator.comparingLong(StandingRanks::scoreOf).reversed())
                        .toList();
        List<RankedRow> ranked = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            StandingRow row = sorted.get(index);
            boolean tiesPrevious = index > 0 && scoreOf(sorted.get(index - 1)) == scoreOf(row);
            int rank = tiesPrevious ? ranked.get(index - 1).rank() : index + 1;
            ranked.add(new RankedRow(rank, row));
        }
        return ranked;
    }

    private static long scoreOf(StandingRow row) {
        return IcpcScoring.rankingScore(row.solved(), row.penaltyMinutes());
    }
}
