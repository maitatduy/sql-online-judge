package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Difficulty;
import java.util.List;
import java.util.UUID;

public record ProblemSummaryResponse(
        UUID id,
        int number,
        String slug,
        String title,
        Difficulty difficulty,
        List<TagResponse> tags,
        double acceptanceRate,
        long solvedCount,
        ProblemProgress progress) {

    public ProblemSummaryResponse {
        tags = List.copyOf(tags);
    }
}
