package com.sqljudge.auth.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "oauth_accounts",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_oauth_accounts_provider_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class OAuthAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            columnDefinition = "BINARY(16)",
            foreignKey = @ForeignKey(name = "fk_oauth_accounts_users"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 10)
    private SignupProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;
}
