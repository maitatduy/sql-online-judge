package com.sqljudge.community.service;

import com.sqljudge.community.entity.Post;
import com.sqljudge.community.entity.PostCategory;
import com.sqljudge.community.entity.PostStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostSnapshot(
        UUID id,
        UUID authorId,
        PostCategory category,
        PostStatus status,
        String title,
        String bodyMarkdown,
        List<String> tags,
        UUID problemId,
        int voteScore,
        int commentCount,
        long viewCount,
        boolean pinned,
        Instant publishedAt,
        Instant updatedAt) {

    static PostSnapshot of(Post post) {
        return new PostSnapshot(
                post.getId(),
                post.getAuthorId(),
                post.getCategory(),
                post.getStatus(),
                post.getTitle(),
                post.getBodyMarkdown(),
                List.copyOf(post.getTags()),
                post.getProblemId(),
                post.getVoteScore(),
                post.getCommentCount(),
                post.getViewCount(),
                post.isPinned(),
                post.getPublishedAt(),
                post.getUpdatedAt());
    }
}
