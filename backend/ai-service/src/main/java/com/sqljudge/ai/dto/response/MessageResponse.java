package com.sqljudge.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.ai.entity.AiMessage;
import com.sqljudge.ai.entity.MessageRole;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        MessageRole role,
        String content,
        @JsonProperty("isBlocked") boolean isBlocked,
        Instant createdAt) {

    public static MessageResponse from(AiMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.isBlocked(),
                message.getCreatedAt());
    }
}
