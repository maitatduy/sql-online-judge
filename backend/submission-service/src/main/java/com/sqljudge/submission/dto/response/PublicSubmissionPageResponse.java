package com.sqljudge.submission.dto.response;

import java.util.List;

public record PublicSubmissionPageResponse(
        List<PublicSubmissionResponse> items, String nextCursor) {

    public PublicSubmissionPageResponse {
        items = List.copyOf(items);
    }
}
