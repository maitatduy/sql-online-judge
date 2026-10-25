package com.sqljudge.auth.repository;

import com.sqljudge.auth.entity.RoleInvitation;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleInvitationRepository extends JpaRepository<RoleInvitation, UUID> {

    @Query(
            """
            SELECT COUNT(i) > 0
            FROM RoleInvitation i
            WHERE i.email = :email
              AND i.acceptedAt IS NULL
              AND i.revokedAt IS NULL
              AND i.expiresAt > :now
            """)
    boolean hasPendingInvitation(@Param("email") String email, @Param("now") Instant now);

    Optional<RoleInvitation> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM RoleInvitation i WHERE i.tokenHash = :tokenHash")
    Optional<RoleInvitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM RoleInvitation i WHERE i.id = :id")
    Optional<RoleInvitation> findByIdForUpdate(@Param("id") UUID id);

    @Query(
            value =
                    """
                    SELECT new com.sqljudge.auth.repository.RoleInvitationRow(
                        i.id, i.email, i.role, inviter.username, i.createdAt, i.expiresAt,
                        i.acceptedAt, i.revokedAt)
                    FROM RoleInvitation i
                    JOIN User inviter ON inviter.id = i.invitedBy
                    WHERE (:keyword IS NULL OR i.email LIKE :keyword ESCAPE '\\')
                      AND (:status IS NULL
                        OR (:status = 'ACCEPTED' AND i.acceptedAt IS NOT NULL)
                        OR (:status = 'REVOKED' AND i.acceptedAt IS NULL AND i.revokedAt IS NOT NULL)
                        OR (:status = 'PENDING' AND i.acceptedAt IS NULL AND i.revokedAt IS NULL
                            AND i.expiresAt > :now)
                        OR (:status = 'EXPIRED' AND i.acceptedAt IS NULL AND i.revokedAt IS NULL
                            AND i.expiresAt <= :now))
                    ORDER BY i.createdAt DESC, i.id DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(i)
                    FROM RoleInvitation i
                    WHERE (:keyword IS NULL OR i.email LIKE :keyword ESCAPE '\\')
                      AND (:status IS NULL
                        OR (:status = 'ACCEPTED' AND i.acceptedAt IS NOT NULL)
                        OR (:status = 'REVOKED' AND i.acceptedAt IS NULL AND i.revokedAt IS NOT NULL)
                        OR (:status = 'PENDING' AND i.acceptedAt IS NULL AND i.revokedAt IS NULL
                            AND i.expiresAt > :now)
                        OR (:status = 'EXPIRED' AND i.acceptedAt IS NULL AND i.revokedAt IS NULL
                            AND i.expiresAt <= :now))
                    """)
    Page<RoleInvitationRow> searchInvitations(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("now") Instant now,
            Pageable pageable);
}
