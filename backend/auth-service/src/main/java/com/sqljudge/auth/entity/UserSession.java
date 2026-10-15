package com.sqljudge.auth.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "user_sessions",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_user_sessions_token_hash", columnNames = "token_hash"),
        indexes = {
            @Index(
                    name = "idx_user_sessions_previous_token_hash",
                    columnList = "previous_token_hash"),
            @Index(
                    name = "idx_user_sessions_user_id_revoked_at",
                    columnList = "user_id, revoked_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class UserSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            columnDefinition = "BINARY(16)",
            foreignKey = @ForeignKey(name = "fk_user_sessions_users"))
    private User user;

    @Column(name = "token_hash", nullable = false, columnDefinition = "CHAR(64)")
    private String tokenHash;

    @Column(name = "previous_token_hash", columnDefinition = "CHAR(64)")
    private String previousTokenHash;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @Column(name = "is_persistent", nullable = false)
    private boolean isPersistent;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isActiveAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public boolean wasRotatedWithin(Duration window, Instant now) {
        return rotatedAt != null && !rotatedAt.plus(window).isBefore(now);
    }

    public void rotateTo(String newTokenHash, Instant now) {
        previousTokenHash = tokenHash;
        tokenHash = newTokenHash;
        rotatedAt = now;
        lastUsedAt = now;
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}
