package com.sqljudge.contest.entity;

import com.sqljudge.common.persistence.BaseEntity;
import com.sqljudge.contest.scoring.EloRating;
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
        name = "user_ratings",
        indexes = @Index(name = "uk_user_ratings_user_id", columnList = "user_id", unique = true))
@Getter
@NoArgsConstructor
public class UserRating extends BaseEntity {

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "contests_played", nullable = false)
    private int contestsPlayed;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public static UserRating initial(UUID userId) {
        UserRating rating = new UserRating();
        rating.userId = userId;
        rating.rating = EloRating.INITIAL_RATING;
        return rating;
    }

    public void record(int newRating) {
        rating = newRating;
        contestsPlayed++;
    }
}
