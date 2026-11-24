package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Difficulty;
import java.util.Map;
import java.util.UUID;

public record SolvedSummaryResponse(
        UUID userId, long solvedCount, Map<Difficulty, SolvedProgress> byDifficulty) {

    public SolvedSummaryResponse {
        byDifficulty = Map.copyOf(byDifficulty);
    }

    public record SolvedProgress(long solved, long total) {}
}
