package com.sqljudge.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProblemUsageResponse(
        @JsonProperty("isInUnfinishedContest") boolean isInUnfinishedContest) {}
