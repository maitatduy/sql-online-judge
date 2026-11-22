package com.sqljudge.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.sqljudge.contest.client.ContestStatement;

public record ContestProblemStatementResponse(
        String letter, @JsonUnwrapped ContestStatement statement) {}
