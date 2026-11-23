package com.sqljudge.contest.dto.response;

import com.sqljudge.contest.entity.ContestRatingChange;

public record RatingChangeResponse(
        String username, int rank, int oldRating, int newRating, int delta) {

    public static RatingChangeResponse from(ContestRatingChange change) {
        return new RatingChangeResponse(
                change.getUsername(),
                change.getRank(),
                change.getOldRating(),
                change.getNewRating(),
                change.getDelta());
    }
}
