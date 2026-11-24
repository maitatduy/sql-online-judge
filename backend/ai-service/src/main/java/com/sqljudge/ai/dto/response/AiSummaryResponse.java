package com.sqljudge.ai.dto.response;

public record AiSummaryResponse(
        long questionsToday,
        long blockedToday,
        long activeUsersToday,
        int dailyMessageLimit,
        String model) {}
