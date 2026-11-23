package com.sqljudge.community.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(
        name = "posts",
        indexes = {
            @Index(name = "idx_posts_status_published_at", columnList = "status, published_at"),
            @Index(
                    name = "idx_posts_category_status_published_at",
                    columnList = "category, status, published_at"),
            @Index(name = "idx_posts_problem_id_category", columnList = "problem_id, category"),
            @Index(name = "idx_posts_author_id", columnList = "author_id")
        })
@Getter
@NoArgsConstructor
public class Post extends BaseEntity {

    @Column(name = "author_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private PostCategory category;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body_markdown", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String bodyMarkdown;

    @Column(name = "problem_id", columnDefinition = "BINARY(16)")
    private UUID problemId;

    @Column(name = "problem_slug", length = 160)
    private String problemSlug;

    @Column(name = "problem_title", length = 200)
    private String problemTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PostStatus status;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    @Column(name = "vote_score", nullable = false)
    private int voteScore;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "hidden_reason", length = 16)
    private HiddenReason hiddenReason;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @ElementCollection
    @BatchSize(size = 50)
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag", nullable = false, length = 40)
    private Set<String> tags = new LinkedHashSet<>();

    public static Post draft(UUID authorId, PostContent content) {
        Post post = new Post();
        post.authorId = authorId;
        post.status = PostStatus.DRAFT;
        post.update(content);
        return post;
    }

    public void update(PostContent content) {
        category = content.category();
        title = content.title();
        bodyMarkdown = content.bodyMarkdown();
        problemId = content.problem() == null ? null : content.problem().id();
        problemSlug = content.problem() == null ? null : content.problem().slug();
        problemTitle = content.problem() == null ? null : content.problem().title();
        tags.clear();
        tags.addAll(content.tags());
    }

    public void publish(Instant now) {
        if (status == PostStatus.DRAFT) {
            status = PostStatus.PUBLISHED;
            publishedAt = now;
        }
    }

    public void hide(HiddenReason reason) {
        if (status == PostStatus.PUBLISHED) {
            status = PostStatus.HIDDEN;
            hiddenReason = reason;
        }
    }

    public void unhide(HiddenReason reason) {
        if (status == PostStatus.HIDDEN && hiddenReason == reason) {
            status = PostStatus.PUBLISHED;
            hiddenReason = null;
        }
    }

    public void delete(Instant now) {
        deletedAt = now;
    }

    public void addVotes(int delta) {
        voteScore += delta;
    }

    public void commentAdded() {
        commentCount++;
    }

    public void commentRemoved() {
        commentCount = Math.max(0, commentCount - 1);
    }

    public boolean isWrittenBy(UUID userId) {
        return authorId.equals(userId);
    }

    public boolean isVisibleToEveryone() {
        return deletedAt == null && status == PostStatus.PUBLISHED;
    }

    public record PostContent(
            PostCategory category,
            String title,
            String bodyMarkdown,
            LinkedProblem problem,
            Set<String> tags) {}
}
