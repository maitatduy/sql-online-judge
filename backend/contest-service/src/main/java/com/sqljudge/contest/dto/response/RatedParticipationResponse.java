package com.sqljudge.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RatedParticipationResponse(
        @JsonProperty("isInRatedContest") boolean isInRatedContest) {}
