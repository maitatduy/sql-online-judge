package com.sqljudge.auth.repository;

import com.sqljudge.auth.dto.response.AuditLogEntryResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
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
                      AND a.action IN :actions
                      AND (:keyword IS NULL
                        OR actor.username LIKE :keyword ESCAPE '\\'
                        OR target.username LIKE :keyword ESCAPE '\\'
                        OR a.reason LIKE :keyword ESCAPE '\\')
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
                      AND a.action IN :actions
                      AND (:keyword IS NULL
                        OR actor.username LIKE :keyword ESCAPE '\\'
                        OR target.username LIKE :keyword ESCAPE '\\'
                        OR a.reason LIKE :keyword ESCAPE '\\')
                      AND (:from IS NULL OR a.createdAt >= :from)
                    """)
    Page<AuditLogEntryResponse> searchAuditLog(
            @Param("targetUsername") String targetUsername,
            @Param("actorUsername") String actorUsername,
            @Param("actions") Collection<AdminActionType> actions,
            @Param("keyword") String keyword,
            @Param("from") Instant from,
            Pageable pageable);

    @Query(
            """
            SELECT new com.sqljudge.auth.dto.response.AuditLogEntryResponse(
                a.id, a.createdAt, actor.username, actor.role, target.username,
                a.action, a.reason, a.previousValue, a.newValue, a.expiresAt, a.details)
            FROM AdminAction a
            JOIN User actor ON actor.id = a.actorId
            LEFT JOIN User target ON target.id = a.targetUserId
            WHERE (:targetUsername IS NULL OR target.username = :targetUsername)
              AND (:actorUsername IS NULL OR actor.username = :actorUsername)
              AND a.action IN :actions
              AND (:keyword IS NULL
                OR actor.username LIKE :keyword ESCAPE '\\'
                OR target.username LIKE :keyword ESCAPE '\\'
                OR a.reason LIKE :keyword ESCAPE '\\')
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:beforeAt IS NULL
                OR a.createdAt < :beforeAt
                OR (a.createdAt = :beforeAt AND a.id < :beforeId))
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<AuditLogEntryResponse> findAuditLogBatch(
            @Param("targetUsername") String targetUsername,
            @Param("actorUsername") String actorUsername,
            @Param("actions") Collection<AdminActionType> actions,
            @Param("keyword") String keyword,
            @Param("from") Instant from,
            @Param("beforeAt") Instant beforeAt,
            @Param("beforeId") UUID beforeId,
            Limit limit);

    @Query(
            """
            SELECT DISTINCT actor.username
            FROM AdminAction a
            JOIN User actor ON actor.id = a.actorId
            ORDER BY actor.username
            """)
    List<String> findActorUsernames();
}
