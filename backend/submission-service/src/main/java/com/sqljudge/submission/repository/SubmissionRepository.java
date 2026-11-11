package com.sqljudge.submission.repository;

import com.sqljudge.submission.dto.response.SubmissionSummaryResponse;
import com.sqljudge.submission.entity.Submission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Optional<Submission> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    Optional<Submission> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @Query(
            HISTORY_SELECT
                    + " WHERE s.userId = :userId AND s.problemId = :problemId"
                    + " AND s.deletedAt IS NULL ORDER BY s.id DESC")
    List<SubmissionSummaryResponse> findHistory(
            @Param("userId") UUID userId, @Param("problemId") UUID problemId, Limit limit);

    @Query(
            HISTORY_SELECT
                    + " WHERE s.userId = :userId AND s.problemId = :problemId"
                    + " AND s.deletedAt IS NULL AND s.id < :beforeId ORDER BY s.id DESC")
    List<SubmissionSummaryResponse> findHistoryBefore(
            @Param("userId") UUID userId,
            @Param("problemId") UUID problemId,
            @Param("beforeId") UUID beforeId,
            Limit limit);

    String HISTORY_SELECT =
            "SELECT new com.sqljudge.submission.dto.response.SubmissionSummaryResponse("
                    + "s.id, s.status, s.verdict, s.dialect, s.runtimeMs, s.createdAt)"
                    + " FROM Submission s";
}
