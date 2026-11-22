package com.sqljudge.contest.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record ContestantInfo(
        UUID userId,
        String username,
        String displayName,
        @JsonProperty("isBannedFromRatedContests") boolean isBannedFromRatedContests) {}
