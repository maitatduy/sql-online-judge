package com.sqljudge.contest.dto.response;

import java.time.Instant;

public record RatingPointResponse(
        String contestSlug, String contestTitle, int rating, int rank, Instant finishedAt) {}
