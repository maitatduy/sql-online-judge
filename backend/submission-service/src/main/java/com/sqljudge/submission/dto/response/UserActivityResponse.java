package com.sqljudge.submission.dto.response;

import java.util.List;

public record UserActivityResponse(long totalSubmissions, List<ActivityDayResponse> days) {

    public UserActivityResponse {
        days = List.copyOf(days);
    }
}
