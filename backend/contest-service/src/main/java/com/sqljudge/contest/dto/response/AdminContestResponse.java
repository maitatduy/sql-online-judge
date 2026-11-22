package com.sqljudge.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.sqljudge.contest.entity.Contest;
import java.time.Instant;
import java.util.List;

public record AdminContestResponse(
        @JsonUnwrapped ContestResponse contest, List<ContestProblemResponse> problems) {

    public AdminContestResponse {
        problems = List.copyOf(problems);
    }

    public static AdminContestResponse from(Contest contest, Instant now) {
        return new AdminContestResponse(
                ContestResponse.from(contest, now),
                contest.getProblems().stream().map(ContestProblemResponse::from).toList());
    }
}
