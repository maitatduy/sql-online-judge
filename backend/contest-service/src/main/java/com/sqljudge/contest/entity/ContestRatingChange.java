package com.sqljudge.contest.entity;

import com.sqljudge.common.persistence.BaseEntity;
import com.sqljudge.contest.scoring.EloRating.Change;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "contest_rating_changes",
        indexes = {
            @Index(
                    name = "uk_contest_rating_changes_contest_id_user_id",
                    columnList = "contest_id, user_id",
                    unique = true),
            @Index(
                    name = "idx_contest_rating_changes_contest_id_rank_position",
                    columnList = "contest_id, rank_position")
        })
@Getter
@NoArgsConstructor
public class ContestRatingChange extends BaseEntity {

    @Column(name = "contest_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID contestId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "rank_position", nullable = false)
    private int rank;

    @Column(name = "old_rating", nullable = false)
    private int oldRating;

    @Column(name = "new_rating", nullable = false)
    private int newRating;

    @Column(name = "delta", nullable = false)
    private int delta;

    public static ContestRatingChange of(UUID contestId, String username, Change change) {
        ContestRatingChange ratingChange = new ContestRatingChange();
        ratingChange.contestId = contestId;
        ratingChange.userId = change.userId();
        ratingChange.username = username;
        ratingChange.rank = change.rank();
        ratingChange.oldRating = change.oldRating();
        ratingChange.newRating = change.newRating();
        ratingChange.delta = change.delta();
        return ratingChange;
    }
}
