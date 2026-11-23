package com.sqljudge.problem.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProblemUsage(@JsonProperty("isInUnfinishedContest") boolean isInUnfinishedContest) {}
