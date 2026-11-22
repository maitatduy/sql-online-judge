package com.sqljudge.contest.dto.response;

import com.sqljudge.contest.entity.ContestProblem;
import java.util.UUID;

public record ContestProblemResponse(
        String letter, UUID problemId, String slug, String title, String difficulty) {

    public static ContestProblemResponse from(ContestProblem problem) {
        return new ContestProblemResponse(
                problem.letter(),
                problem.getProblemId(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getDifficulty());
    }
}
