package com.sqljudge.problem.dto.response;

import java.util.List;

public record DailyChallengesResponse(List<ProblemSummaryResponse> items) {

    public DailyChallengesResponse {
        items = List.copyOf(items);
    }
}
