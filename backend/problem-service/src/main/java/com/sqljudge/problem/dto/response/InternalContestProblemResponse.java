package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.Problem;
import java.util.UUID;

public record InternalContestProblemResponse(
        UUID problemId, String slug, String title, Difficulty difficulty) {

    public static InternalContestProblemResponse from(Problem problem) {
        return new InternalContestProblemResponse(
                problem.getId(), problem.getSlug(), problem.getTitle(), problem.getDifficulty());
    }
}
