package com.sqljudge.auth.entity;

import com.sqljudge.common.persistence.BaseEntity;
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
        indexes =
                @Index(
                        name = "idx_admin_actions_target_user_id_created_at",
                        columnList = "target_user_id, created_at"))
@Getter
@Setter
@NoArgsConstructor
public class AdminAction extends BaseEntity {

    @Column(name = "actor_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID actorId;

    @Column(name = "target_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private AdminActionType action;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public static AdminAction ban(UUID actorId, User target, String reason) {
        AdminAction action = of(actorId, target, AdminActionType.BAN, reason);
        action.expiresAt = target.getBannedUntil();
        return action;
    }

    public static AdminAction unban(UUID actorId, User target, String reason) {
        return of(actorId, target, AdminActionType.UNBAN, reason);
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
