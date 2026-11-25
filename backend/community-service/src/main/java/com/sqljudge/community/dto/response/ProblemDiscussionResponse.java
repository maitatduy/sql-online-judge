package com.sqljudge.community.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProblemDiscussionResponse(
        UUID commentId,
        UUID problemId,
        String problemSlug,
        String problemTitle,
        UUID authorId,
        String authorUsername,
        String excerpt,
        boolean isReply,
        Instant createdAt) {}
