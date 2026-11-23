package com.sqljudge.contest.entity;

import com.sqljudge.common.persistence.BaseEntity;
import com.sqljudge.contest.scoring.FinalRanks.Ranked;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "contest_standings",
        indexes = {
            @Index(
                    name = "uk_contest_standings_contest_id_user_id",
                    columnList = "contest_id, user_id",
                    unique = true),
            @Index(
                    name = "idx_contest_standings_contest_id_rank_position",
                    columnList = "contest_id, rank_position")
        })
@Getter
@NoArgsConstructor
public class ContestStanding extends BaseEntity {

    @Column(name = "contest_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID contestId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "rank_position", nullable = false)
    private int rank;

    @Column(name = "solved", nullable = false)
    private int solved;

    @Column(name = "penalty_minutes", nullable = false)
    private long penaltyMinutes;

    public static ContestStanding of(ContestRegistration registration, Ranked ranked) {
        ContestStanding standing = new ContestStanding();
        standing.contestId = registration.getContest().getId();
        standing.userId = registration.getUserId();
        standing.username = registration.getUsername();
        standing.displayName = registration.getDisplayName();
        standing.rank = ranked.rank();
        standing.solved = ranked.solved();
        standing.penaltyMinutes = ranked.penaltyMinutes();
        return standing;
    }
}
