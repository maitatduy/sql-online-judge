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
import lombok.Setter;

@Entity
@Table(
        name = "admin_actions",
        indexes = {
            @Index(
                    name = "idx_admin_actions_target_user_id_created_at",
                    columnList = "target_user_id, created_at"),
            @Index(name = "idx_admin_actions_created_at", columnList = "created_at"),
            @Index(
                    name = "idx_admin_actions_actor_id_created_at",
                    columnList = "actor_id, created_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class AdminAction extends BaseEntity {

    @Column(name = "actor_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID actorId;

    @Column(name = "target_user_id", columnDefinition = "BINARY(16)")
    private UUID targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private AdminActionType action;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "previous_value", length = 100)
    private String previousValue;

    @Column(name = "new_value", length = 100)
    private String newValue;

    @Column(name = "details", length = 500)
    private String details;

    public static AdminAction ban(UUID actorId, User target, String reason) {
        AdminAction action = of(actorId, target, AdminActionType.BAN, requireReason(reason));
        action.expiresAt = target.getBannedUntil();
        return action;
    }

    public static AdminAction unban(UUID actorId, User target, String reason) {
        return of(actorId, target, AdminActionType.UNBAN, requireReason(reason));
    }

    public static AdminAction changeRole(UUID actorId, User target, ValueChange change) {
        AdminAction action =
                of(actorId, target, AdminActionType.UPDATE_ROLE, requireReason(change.reason()));
        action.previousValue = change.previous();
        action.newValue = target.getRole().name();
        return action;
    }

    public static AdminAction changeDisplayName(UUID actorId, User target, ValueChange change) {
        AdminAction action =
                of(actorId, target, AdminActionType.UPDATE_DISPLAY_NAME, change.reason());
        action.previousValue = change.previous();
        action.newValue = target.getDisplayName();
        return action;
    }

    public static AdminAction revokeSession(UUID actorId, User target) {
        return of(actorId, target, AdminActionType.REVOKE_SESSION, null);
    }

    public static AdminAction revokeAllSessions(UUID actorId, User target) {
        return of(actorId, target, AdminActionType.REVOKE_ALL_SESSIONS, null);
    }

    public static AdminAction sendPasswordReset(UUID actorId, User target) {
        return of(actorId, target, AdminActionType.SEND_PASSWORD_RESET, null);
    }

    public static AdminAction sendVerification(UUID actorId, User target) {
        return of(actorId, target, AdminActionType.SEND_VERIFICATION, null);
    }

    public static AdminAction delete(UUID actorId, User target, String reason) {
        return of(actorId, target, AdminActionType.DELETE, requireReason(reason));
    }

    public static AdminAction restore(UUID actorId, User target) {
        return of(actorId, target, AdminActionType.RESTORE, null);
    }

    public static AdminAction updateAvatar(UUID actorId, User target) {
        return of(actorId, target, AdminActionType.UPDATE_AVATAR, null);
    }

    public static AdminAction removeAvatar(UUID actorId, User target) {
        return of(actorId, target, AdminActionType.REMOVE_AVATAR, null);
    }

    public static AdminAction updateRestrictions(
            UUID actorId, User target, RestrictionsChange change) {
        AdminAction action =
                of(
                        actorId,
                        target,
                        AdminActionType.UPDATE_RESTRICTIONS,
                        requireReason(change.reason()));
        action.previousValue = change.previous();
        action.newValue = change.current();
        return action;
    }

    public static AdminAction exportUserEmails(UUID actorId, String reason, String details) {
        AdminAction action = new AdminAction();
        action.actorId = actorId;
        action.action = AdminActionType.EXPORT_USER_EMAILS;
        action.reason = requireReason(reason);
        action.details = details;
        return action;
    }

    public static AdminAction exportAuditLog(UUID actorId, String details) {
        AdminAction action = new AdminAction();
        action.actorId = actorId;
        action.action = AdminActionType.EXPORT_AUDIT_LOG;
        action.details = details;
        return action;
    }

    public static AdminAction inviteRole(UUID actorId, User invitee, RoleInvitation invitation) {
        AdminAction action = new AdminAction();
        action.actorId = actorId;
        action.action = AdminActionType.INVITE_ROLE;
        action.newValue = invitation.getRole().name();
        if (invitee == null) {
            action.details = invitation.getEmail();
        } else {
            action.targetUserId = invitee.getId();
        }
        return action;
    }

    public static AdminAction acceptRoleInvitation(User invitee, Role previousRole) {
        AdminAction action =
                of(invitee.getId(), invitee, AdminActionType.ACCEPT_ROLE_INVITATION, null);
        action.previousValue = previousRole.name();
        action.newValue = invitee.getRole().name();
        return action;
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required for this admin action");
        }
        return reason;
    }

    private static AdminAction of(UUID actorId, User target, AdminActionType type, String reason) {
        AdminAction action = new AdminAction();
        action.actorId = actorId;
        action.targetUserId = target.getId();
        action.action = type;
        action.reason = reason;
        return action;
    }
}
