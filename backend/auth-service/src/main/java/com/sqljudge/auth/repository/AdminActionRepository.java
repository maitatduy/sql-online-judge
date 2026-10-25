package com.sqljudge.auth.repository;

import com.sqljudge.auth.dto.response.AuditLogEntryResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminActionRepository extends JpaRepository<AdminAction, UUID> {

    @Query(
            value =
                    """
                    SELECT new com.sqljudge.auth.dto.response.AuditLogEntryResponse(
                        a.id, a.createdAt, actor.username, actor.role, target.username,
                        a.action, a.reason, a.previousValue, a.newValue, a.expiresAt, a.details)
                    FROM AdminAction a
                    JOIN User actor ON actor.id = a.actorId
                    LEFT JOIN User target ON target.id = a.targetUserId
                    WHERE (:targetUsername IS NULL OR target.username = :targetUsername)
                      AND (:actorUsername IS NULL OR actor.username = :actorUsername)
                      AND (:action IS NULL OR a.action = :action)
                      AND (:from IS NULL OR a.createdAt >= :from)
                    ORDER BY a.createdAt DESC, a.id DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(a)
                    FROM AdminAction a
                    JOIN User actor ON actor.id = a.actorId
                    LEFT JOIN User target ON target.id = a.targetUserId
                    WHERE (:targetUsername IS NULL OR target.username = :targetUsername)
                      AND (:actorUsername IS NULL OR actor.username = :actorUsername)
                      AND (:action IS NULL OR a.action = :action)
                      AND (:from IS NULL OR a.createdAt >= :from)
                    """)
    Page<AuditLogEntryResponse> searchAuditLog(
            @Param("targetUsername") String targetUsername,
            @Param("actorUsername") String actorUsername,
            @Param("action") AdminActionType action,
            @Param("from") Instant from,
            Pageable pageable);
}
