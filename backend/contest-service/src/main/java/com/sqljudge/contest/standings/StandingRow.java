package com.sqljudge.contest.standings;

import java.util.List;
import java.util.UUID;

public record StandingRow(
        UUID userId,
        String username,
        String displayName,
        int solved,
        long penaltyMinutes,
        List<StandingCell> cells) {

    public StandingRow {
        cells = List.copyOf(cells);
    }

    public record StandingCell(
            UUID problemId, Integer acceptedMinute, int wrongAttempts, int pendingAttempts) {}
}
