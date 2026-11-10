package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "problem_reviews",
        indexes =
                @Index(
                        name = "idx_problem_reviews_problem_id_created_at",
                        columnList = "problem_id, created_at"))
@Getter
@NoArgsConstructor
public class ProblemReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private ProblemRevision revision;

    @Column(name = "reviewer_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private ReviewDecision decision;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    ProblemReview(Problem problem, UUID reviewerId, ReviewDecision decision, String reason) {
        this.problem = problem;
        this.revision = problem.getDraftRevision();
        this.reviewerId = reviewerId;
        this.decision = decision;
        this.reason = reason;
    }
}
