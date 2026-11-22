package com.sqljudge.contest.entity;

import com.sqljudge.common.persistence.BaseEntity;
import com.sqljudge.contest.scoring.Attempt;
import com.sqljudge.contest.scoring.JudgeVerdict;
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
        name = "contest_attempts",
        indexes = {
            @Index(
                    name = "uk_contest_attempts_submission_id",
                    columnList = "submission_id",
                    unique = true),
            @Index(
                    name = "idx_contest_attempts_contest_id_user_id_problem_id",
                    columnList = "contest_id, user_id, problem_id")
        })
@Getter
@NoArgsConstructor
public class ContestAttempt extends BaseEntity {

    @Column(name = "contest_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID contestId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "problem_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID problemId;

    @Column(name = "submission_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID submissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 8)
    private JudgeVerdict verdict;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public static ContestAttempt of(ContestCellKey key, UUID submissionId, Attempt attempt) {
        ContestAttempt contestAttempt = new ContestAttempt();
        contestAttempt.contestId = key.contestId();
        contestAttempt.userId = key.userId();
        contestAttempt.problemId = key.problemId();
        contestAttempt.submissionId = submissionId;
        contestAttempt.verdict = attempt.verdict();
        contestAttempt.submittedAt = attempt.submittedAt();
        return contestAttempt;
    }

    public Attempt toAttempt() {
        return new Attempt(submittedAt, verdict);
    }
}
