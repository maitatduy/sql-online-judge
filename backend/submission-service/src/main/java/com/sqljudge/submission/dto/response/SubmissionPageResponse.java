package com.sqljudge.submission.dto.response;

import java.util.List;

public record SubmissionPageResponse(List<SubmissionSummaryResponse> items, String nextCursor) {

    public SubmissionPageResponse {
        items = List.copyOf(items);
    }
}
