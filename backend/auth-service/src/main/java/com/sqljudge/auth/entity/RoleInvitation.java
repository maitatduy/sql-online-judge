package com.sqljudge.auth.entity;

import com.sqljudge.common.persistence.BaseEntity;
import com.sqljudge.common.security.Role;
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
        name = "role_invitations",
        indexes = {
            @Index(
                    name = "uk_role_invitations_token_hash",
                    columnList = "token_hash",
                    unique = true),
            @Index(
                    name = "idx_role_invitations_email_created_at",
                    columnList = "email, created_at"),
            @Index(name = "idx_role_invitations_created_at", columnList = "created_at")
        })
@Getter
@NoArgsConstructor
public class RoleInvitation extends BaseEntity {

    @Column(name = "email", nullable = false, updatable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, updatable = false, length = 20)
    private Role role;

    @Column(name = "message", updatable = false, length = 500)
    private String message;

    @Column(name = "token_hash", nullable = false, columnDefinition = "CHAR(64)")
    private String tokenHash;

    @Column(
            name = "invited_by",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID invitedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "accepted_by", columnDefinition = "BINARY(16)")
    private UUID acceptedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public static RoleInvitation of(
            String email, Role role, String message, UUID invitedBy, InvitationLink link) {
        RoleInvitation invitation = new RoleInvitation();
        invitation.email = email;
        invitation.role = role;
        invitation.message = message;
        invitation.invitedBy = invitedBy;
        invitation.tokenHash = link.tokenHash();
        invitation.expiresAt = link.expiresAt();
        return invitation;
    }
}
