package com.sqljudge.contest.dto.response;

import java.util.UUID;

public record LeaderboardEntryResponse(
        int rank,
        UUID userId,
        String username,
        String displayName,
        int rating,
        int contestsPlayed) {}
