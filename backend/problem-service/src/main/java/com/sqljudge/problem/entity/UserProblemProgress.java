package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_problem_progress",
        indexes = {
            @Index(
                    name = "uk_user_problem_progress_user_id_problem_id",
                    columnList = "user_id, problem_id",
                    unique = true),
            @Index(
                    name = "idx_user_problem_progress_user_id_status",
                    columnList = "user_id, status")
        })
@Getter
@NoArgsConstructor
public class UserProblemProgress extends BaseEntity {

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "problem_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID problemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ProblemProgressStatus status;

    @Column(name = "first_solved_at")
    private Instant firstSolvedAt;

    public UserProblemProgress(UUID userId, UUID problemId, ProblemProgressStatus status) {
        this.userId = userId;
        this.problemId = problemId;
        this.status = status;
    }
}
