package com.sqljudge.auth.entity;

import com.sqljudge.common.persistence.BaseEntity;
import com.sqljudge.common.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
            @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        },
        indexes = {
            @Index(name = "idx_users_role", columnList = "role"),
            @Index(name = "idx_users_created_at", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, length = 30)
    private String username;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", columnDefinition = "CHAR(60)")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "signup_provider", nullable = false, length = 10)
    private SignupProvider signupProvider;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "banned_at")
    private Instant bannedAt;

    @Column(name = "banned_until")
    private Instant bannedUntil;

    @Column(name = "ban_reason", length = 500)
    private String banReason;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean isBannedAt(Instant now) {
        return bannedAt != null && (bannedUntil == null || bannedUntil.isAfter(now));
    }

    public boolean canSignInAt(Instant now) {
        return !isDeleted() && isEmailVerified() && !isBannedAt(now);
    }

    public boolean isAwaitingEmailVerification() {
        return emailVerifiedAt == null && deletedAt == null;
    }

    public void markEmailVerified(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
        }
    }
}
