package com.sqljudge.contest.dto.response;

import java.util.UUID;

public record RatingSummaryResponse(UUID userId, Integer rating, Integer rank, int contestsPlayed) {

    public static RatingSummaryResponse unrated(UUID userId) {
        return new RatingSummaryResponse(userId, null, null, 0);
    }
}
