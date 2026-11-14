package com.sqljudge.learning.entity;

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
import lombok.Setter;

@Entity
@Table(
        name = "courses",
        indexes = {
            @Index(name = "uk_courses_slug", columnList = "slug", unique = true),
            @Index(name = "idx_courses_status_author_id", columnList = "status, author_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Course extends BaseEntity {

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "author_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status;

    @Column(name = "published_revision_id", columnDefinition = "BINARY(16)")
    private UUID publishedRevisionId;

    @Column(name = "draft_revision_id", columnDefinition = "BINARY(16)")
    private UUID draftRevisionId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
