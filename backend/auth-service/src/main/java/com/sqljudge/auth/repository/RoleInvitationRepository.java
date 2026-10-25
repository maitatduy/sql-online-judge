package com.sqljudge.auth.repository;

import com.sqljudge.auth.entity.RoleInvitation;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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
}
