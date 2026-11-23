package com.sqljudge.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StandingsResponse(
        List<String> problems,
        @JsonProperty("isFrozen") boolean isFrozen,
        List<StandingItem> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    public StandingsResponse {
        problems = List.copyOf(problems);
        items = List.copyOf(items);
    }

    public record StandingItem(
            int rank,
            String username,
            String displayName,
            int solved,
            long penaltyMinutes,
            List<StandingCellResponse> cells) {}

    public record StandingCellResponse(
            String letter, Integer acceptedMinute, int wrongAttempts, int pendingAttempts) {}

    public record MyStanding(int solved, long penaltyMinutes, List<StandingCellResponse> cells) {}
}
