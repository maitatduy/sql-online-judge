package com.sqljudge.ai.dto.response;

import com.sqljudge.ai.entity.BlockReason;
import java.time.Instant;
import java.util.UUID;

public record AiFlagResponse(
        UUID messageId,
        UUID conversationId,
        UUID userId,
        String username,
        String problemSlug,
        BlockReason reason,
        String question,
        Instant createdAt) {}
