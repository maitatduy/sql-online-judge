package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.OutboxEvent;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    String SKIP_LOCKED = "-2";

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = SKIP_LOCKED))
    @Query(
            "SELECT e FROM OutboxEvent e WHERE e.publishedAt IS NULL"
                    + " AND (e.claimedAt IS NULL OR e.claimedAt < :staleBefore)"
                    + " ORDER BY e.createdAt")
    List<OutboxEvent> findClaimable(@Param("staleBefore") Instant staleBefore, Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.publishedAt = :publishedAt WHERE e.id IN :ids")
    void markPublished(
            @Param("ids") Collection<UUID> ids, @Param("publishedAt") Instant publishedAt);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.claimedAt = NULL WHERE e.id IN :ids")
    void release(@Param("ids") Collection<UUID> ids);
}
