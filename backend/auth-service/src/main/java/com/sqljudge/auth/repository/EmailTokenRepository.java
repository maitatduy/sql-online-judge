package com.sqljudge.auth.repository;

import com.sqljudge.auth.entity.EmailToken;
import com.sqljudge.auth.entity.EmailTokenPurpose;
import com.sqljudge.auth.entity.User;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailTokenRepository extends JpaRepository<EmailToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailToken> findFirstByUserEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            String email, EmailTokenPurpose purpose);

    Optional<EmailToken> findFirstByUserAndPurposeOrderByCreatedAtDesc(
            User user, EmailTokenPurpose purpose);

    @Modifying
    @Query(
            "UPDATE EmailToken t SET t.usedAt = :now"
                    + " WHERE t.user = :user AND t.purpose = :purpose AND t.usedAt IS NULL")
    int invalidateUnusedTokens(
            @Param("user") User user,
            @Param("purpose") EmailTokenPurpose purpose,
            @Param("now") Instant now);

    @Modifying
    @Query("UPDATE EmailToken t SET t.usedAt = :now WHERE t.user = :user AND t.usedAt IS NULL")
    int invalidateAllUnusedTokens(@Param("user") User user, @Param("now") Instant now);
}
