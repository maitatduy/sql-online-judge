package com.sqljudge.ai.dto.response;

import java.util.List;

public record ConversationDetailResponse(
        ConversationSummaryResponse conversation, List<MessageResponse> messages) {

    public ConversationDetailResponse {
        messages = List.copyOf(messages);
    }
}
