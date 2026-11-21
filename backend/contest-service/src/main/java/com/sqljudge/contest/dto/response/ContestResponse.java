package com.sqljudge.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestPhase;
import java.time.Instant;
import java.util.UUID;

public record ContestResponse(
        UUID id,
        String slug,
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        int durationMinutes,
        int freezeMinutes,
        int penaltyMinutes,
        @JsonProperty("isRated") boolean isRated,
        ContestPhase phase) {

    public static ContestResponse from(Contest contest, Instant now) {
        return new ContestResponse(
                contest.getId(),
                contest.getSlug(),
                contest.getTitle(),
                contest.getDescription(),
                contest.getStartAt(),
                contest.getEndAt(),
                contest.getDurationMinutes(),
                contest.getFreezeMinutes(),
                contest.getPenaltyMinutes(),
                contest.isRated(),
                contest.phaseAt(now));
    }
}
