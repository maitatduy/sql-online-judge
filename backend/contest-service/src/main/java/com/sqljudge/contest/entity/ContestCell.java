package com.sqljudge.contest.entity;

import com.sqljudge.common.persistence.BaseEntity;
import com.sqljudge.contest.scoring.ProblemScore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "contest_cells",
        indexes =
                @Index(
                        name = "uk_contest_cells_contest_id_user_id_problem_id",
                        columnList = "contest_id, user_id, problem_id",
                        unique = true))
@Getter
@NoArgsConstructor
public class ContestCell extends BaseEntity {

    @Column(name = "contest_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID contestId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "problem_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID problemId;

    @Column(name = "accepted_minute")
    private Integer acceptedMinute;

    @Column(name = "wrong_attempts", nullable = false)
    private int wrongAttempts;

    @Column(name = "penalty_minutes", nullable = false)
    private int penaltyMinutes;

    @Column(name = "public_accepted_minute")
    private Integer publicAcceptedMinute;

    @Column(name = "public_wrong_attempts", nullable = false)
    private int publicWrongAttempts;

    @Column(name = "public_penalty_minutes", nullable = false)
    private int publicPenaltyMinutes;

    @Column(name = "pending_attempts", nullable = false)
    private int pendingAttempts;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public static ContestCell empty(ContestCellKey key) {
        ContestCell cell = new ContestCell();
        cell.contestId = key.contestId();
        cell.userId = key.userId();
        cell.problemId = key.problemId();
        return cell;
    }

    public void update(ProblemScore actual, ProblemScore shown) {
        acceptedMinute = actual.acceptedMinute();
        wrongAttempts = actual.wrongAttempts();
        penaltyMinutes = actual.penaltyMinutes();
        publicAcceptedMinute = shown.acceptedMinute();
        publicWrongAttempts = shown.wrongAttempts();
        publicPenaltyMinutes = shown.penaltyMinutes();
        pendingAttempts = shown.pendingAttempts();
    }
}
