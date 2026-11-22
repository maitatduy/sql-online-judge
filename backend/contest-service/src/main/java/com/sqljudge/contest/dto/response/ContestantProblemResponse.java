package com.sqljudge.contest.dto.response;

import com.sqljudge.contest.entity.ContestProblem;

public record ContestantProblemResponse(String letter, String title, String difficulty) {

    public static ContestantProblemResponse from(ContestProblem problem) {
        return new ContestantProblemResponse(
                problem.letter(), problem.getTitle(), problem.getDifficulty());
    }
}
