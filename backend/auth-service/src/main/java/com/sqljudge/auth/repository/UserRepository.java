package com.sqljudge.auth.repository;

import com.sqljudge.auth.entity.User;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findAllByUsernameIn(Collection<String> usernames);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsernameForUpdate(@Param("username") String username);

    @Query(
            """
            SELECT new com.sqljudge.auth.repository.UserCounts(
                COUNT(u),
                COALESCE(SUM(CASE WHEN u.emailVerifiedAt IS NULL
                    AND (u.bannedAt IS NULL OR u.bannedUntil <= :now) THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN u.role = com.sqljudge.common.security.Role.SETTER
                    THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN u.role = com.sqljudge.common.security.Role.MODERATOR
                    THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN u.bannedAt IS NOT NULL
                    AND (u.bannedUntil IS NULL OR u.bannedUntil > :now) THEN 1L ELSE 0L END), 0L))
            FROM User u
            WHERE u.deletedAt IS NULL
            """)
    UserCounts countLiveUsers(@Param("now") Instant now);
}
