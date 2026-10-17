package com.sqljudge.auth.repository;

import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserSession;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserSession> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserSession> findByPreviousTokenHash(String previousTokenHash);

    @Modifying
    @Query(
            "UPDATE UserSession s SET s.revokedAt = :now"
                    + " WHERE s.user = :user AND s.revokedAt IS NULL")
    int revokeAllActiveSessions(@Param("user") User user, @Param("now") Instant now);
}
