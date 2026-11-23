package com.sqljudge.community.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "votes",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_votes_target_type_target_id_user_id",
                        columnNames = {"target_type", "target_id", "user_id"}),
        indexes =
                @Index(
                        name = "idx_votes_target_author_id_updated_at",
                        columnList = "target_author_id, updated_at"))
@Getter
@NoArgsConstructor
public class Vote extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private VoteTarget targetType;

    @Column(name = "target_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID targetId;

    @Column(name = "target_author_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID targetAuthorId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "vote_value", nullable = false)
    private int value;

    public static Vote cast(
            VoteTarget targetType, UUID targetId, UUID targetAuthorId, UUID userId) {
        Vote vote = new Vote();
        vote.targetType = targetType;
        vote.targetId = targetId;
        vote.targetAuthorId = targetAuthorId;
        vote.userId = userId;
        return vote;
    }

    public int changeTo(int newValue) {
        int delta = newValue - value;
        value = newValue;
        return delta;
    }
}
