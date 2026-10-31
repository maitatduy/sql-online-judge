package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemStatus;
import java.time.Instant;
import java.util.UUID;

public record StudioProblemSummaryResponse(
        UUID id,
        Integer number,
        String slug,
        String title,
        ProblemStatus status,
        Difficulty difficulty,
        Instant updatedAt) {

    public static StudioProblemSummaryResponse from(Problem problem) {
        return new StudioProblemSummaryResponse(
                problem.getId(),
                problem.getNumber(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getStatus(),
                problem.getDifficulty(),
                problem.getUpdatedAt());
    }
}
