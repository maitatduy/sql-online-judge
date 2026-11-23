package com.sqljudge.submission.repository;

import com.sqljudge.submission.entity.RejudgeBatch;
import com.sqljudge.submission.entity.RejudgeStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RejudgeBatchRepository extends JpaRepository<RejudgeBatch, UUID> {

    boolean existsByProblemIdAndStatus(UUID problemId, RejudgeStatus status);

    Optional<RejudgeBatch> findFirstByProblemIdOrderByCreatedAtDesc(UUID problemId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE RejudgeBatch b SET b.remainingCount = b.remainingCount - 1"
                    + " WHERE b.id = :batchId AND b.remainingCount > 0")
    int countDown(@Param("batchId") UUID batchId);
}
