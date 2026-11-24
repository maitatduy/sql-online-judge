package com.sqljudge.ai.dto.response;

import com.sqljudge.ai.entity.AiConversation;
import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryResponse(
        UUID id, String title, UUID problemId, String problemSlug, Instant lastMessageAt) {

    public static ConversationSummaryResponse from(AiConversation conversation) {
        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getProblemId(),
                conversation.getProblemSlug(),
                conversation.getLastMessageAt());
    }
}
