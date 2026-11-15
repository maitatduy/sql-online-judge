package com.sqljudge.submission.repository;

import com.sqljudge.submission.dto.response.SubmissionSummaryResponse;
import com.sqljudge.submission.entity.Submission;
import java.time.Instant;
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

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.userId = :userId AND s.deletedAt IS NULL")
    long countOwn(@Param("userId") UUID userId);

    @Query(
            "SELECT COUNT(s) FROM Submission s WHERE s.userId = :userId AND s.deletedAt IS NULL"
                    + " AND s.verdict = com.sqljudge.submission.entity.Verdict.AC")
    long countOwnAccepted(@Param("userId") UUID userId);

    @Query(
            "SELECT new com.sqljudge.submission.repository.FirstSolveRow(s.problemId,"
                    + " MIN(s.createdAt)) FROM Submission s WHERE s.userId = :userId"
                    + " AND s.deletedAt IS NULL"
                    + " AND s.verdict = com.sqljudge.submission.entity.Verdict.AC"
                    + " GROUP BY s.problemId")
    List<FirstSolveRow> findFirstSolves(@Param("userId") UUID userId);

    @Query(
            "SELECT s.createdAt FROM Submission s WHERE s.userId = :userId"
                    + " AND s.deletedAt IS NULL AND s.createdAt >= :from")
    List<Instant> findCreatedAtSince(@Param("userId") UUID userId, @Param("from") Instant from);

    String HISTORY_SELECT =
            "SELECT new com.sqljudge.submission.dto.response.SubmissionSummaryResponse("
                    + "s.id, s.status, s.verdict, s.dialect, s.runtimeMs, s.createdAt)"
                    + " FROM Submission s";
}
