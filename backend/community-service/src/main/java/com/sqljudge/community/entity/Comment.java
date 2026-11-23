package com.sqljudge.community.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "comments",
        indexes = {
            @Index(name = "idx_comments_post_id_created_at", columnList = "post_id, created_at"),
            @Index(
                    name = "idx_comments_problem_id_created_at",
                    columnList = "problem_id, created_at"),
            @Index(name = "idx_comments_parent_id", columnList = "parent_id")
        })
@Getter
@NoArgsConstructor
public class Comment extends BaseEntity {

    @Column(name = "post_id", columnDefinition = "BINARY(16)")
    private UUID postId;

    @Column(name = "problem_id", columnDefinition = "BINARY(16)")
    private UUID problemId;

    @Column(name = "parent_id", columnDefinition = "BINARY(16)")
    private UUID parentId;

    @Column(name = "author_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID authorId;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "vote_score", nullable = false)
    private int voteScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CommentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "hidden_reason", length = 16)
    private HiddenReason hiddenReason;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public static Comment write(CommentThread thread, UUID parentId, UUID authorId, String body) {
        Comment comment = new Comment();
        comment.postId = thread.postId();
        comment.problemId = thread.problemId();
        comment.parentId = parentId;
        comment.authorId = authorId;
        comment.body = body;
        comment.status = CommentStatus.VISIBLE;
        return comment;
    }

    public CommentThread thread() {
        return new CommentThread(postId, problemId);
    }

    public boolean isReply() {
        return parentId != null;
    }

    public boolean isWrittenBy(UUID userId) {
        return authorId.equals(userId);
    }

    public void edit(String newBody) {
        body = newBody;
    }

    public void hide(HiddenReason reason) {
        if (status == CommentStatus.VISIBLE) {
            status = CommentStatus.HIDDEN;
            hiddenReason = reason;
        }
    }

    public void unhide(HiddenReason reason) {
        if (status == CommentStatus.HIDDEN && hiddenReason == reason) {
            status = CommentStatus.VISIBLE;
            hiddenReason = null;
        }
    }

    public void delete(Instant now) {
        deletedAt = now;
    }

    public void addVotes(int delta) {
        voteScore += delta;
    }

    public record CommentThread(UUID postId, UUID problemId) {

        public boolean isValid() {
            return (postId == null) != (problemId == null);
        }
    }
}
