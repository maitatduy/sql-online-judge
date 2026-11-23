package com.sqljudge.community.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID problemId,
        UUID parentId,
        UUID authorId,
        String authorUsername,
        String body,
        int voteScore,
        int myVote,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> replies) {}
