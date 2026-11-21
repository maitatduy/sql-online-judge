package com.sqljudge.contest.entity;

import java.time.Instant;

public record ContestSchedule(
        String slug,
        String title,
        String description,
        Instant startAt,
        int durationMinutes,
        int freezeMinutes,
        int penaltyMinutes,
        boolean rated) {}
