package com.sqljudge.submission.dto.response;

import java.util.List;

public record JudgeAttentionResponse(List<AdminSubmissionResponse> items) {

    public JudgeAttentionResponse {
        items = List.copyOf(items);
    }
}
