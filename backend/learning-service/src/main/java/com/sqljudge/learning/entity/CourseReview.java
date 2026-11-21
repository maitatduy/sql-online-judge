package com.sqljudge.learning.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "course_reviews",
        indexes = {
            @Index(
                    name = "idx_course_reviews_course_id_created_at",
                    columnList = "course_id, created_at")
        })
@Getter
@NoArgsConstructor
public class CourseReview extends BaseEntity {

    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @Column(name = "revision_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID revisionId;

    @Column(name = "reviewer_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private CourseReviewDecision decision;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    public static CourseReview of(CourseRevision revision, UUID reviewerId) {
        CourseReview review = new CourseReview();
        review.courseId = revision.getCourse().getId();
        review.revisionId = revision.getId();
        review.reviewerId = reviewerId;
        return review;
    }

    public CourseReview decide(CourseReviewDecision decision, String reason) {
        this.decision = decision;
        this.reason = reason;
        return this;
    }
}
