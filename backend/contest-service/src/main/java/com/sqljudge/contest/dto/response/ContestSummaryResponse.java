package com.sqljudge.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        ContestPhase phase,
        long problemCount,
        long registrantCount,
        long participantCount,
        @JsonProperty("isRegistered") boolean isRegistered) {}
