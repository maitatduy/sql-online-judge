package com.sqljudge.ai.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RatedParticipation(@JsonProperty("isInRatedContest") boolean isInRatedContest) {}
