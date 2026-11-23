package com.sqljudge.community.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.community.entity.PostCategory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostSummaryResponse(
        UUID id,
        PostCategory category,
        String title,
        String excerpt,
        List<String> tags,
        UUID problemId,
        UUID authorId,
        String authorUsername,
        int voteScore,
        int commentCount,
        @JsonProperty("isPinned") boolean isPinned,
        Instant publishedAt) {}
