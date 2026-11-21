package com.sqljudge.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestPhase;
import java.time.Instant;
import java.util.UUID;

public record ContestSummaryResponse(
        UUID id,
        String slug,
        String title,
        Instant startAt,
        Instant endAt,
        int durationMinutes,
        @JsonProperty("isRated") boolean isRated,
        ContestPhase phase) {

    public static ContestSummaryResponse from(Contest contest, Instant now) {
        return new ContestSummaryResponse(
                contest.getId(),
                contest.getSlug(),
                contest.getTitle(),
                contest.getStartAt(),
                contest.getEndAt(),
                contest.getDurationMinutes(),
                contest.isRated(),
                contest.phaseAt(now));
    }
}
