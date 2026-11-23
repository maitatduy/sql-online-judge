package com.sqljudge.community.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.community.entity.PostCategory;
import com.sqljudge.community.entity.PostStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostDetailResponse(
        UUID id,
        PostCategory category,
        PostStatus status,
        String title,
        String bodyMarkdown,
        List<String> tags,
        UUID problemId,
        String problemSlug,
        String problemTitle,
        UUID authorId,
        String authorUsername,
        int voteScore,
        int commentCount,
        long viewCount,
        @JsonProperty("isPinned") boolean isPinned,
        Instant publishedAt,
        Instant updatedAt,
        int myVote,
        @JsonProperty("isBookmarked") boolean isBookmarked) {}
