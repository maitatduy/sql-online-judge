package com.sqljudge.submission.repository;

import com.sqljudge.submission.dto.response.PublicSubmissionResponse;
import com.sqljudge.submission.dto.response.RecentSubmissionResponse;
import com.sqljudge.submission.dto.response.SubmissionSummaryResponse;
import com.sqljudge.submission.entity.Submission;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.entity.Verdict;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    String JUDGED_OF_PROBLEM =
            " FROM Submission s WHERE s.problemId = :problemId AND s.status = :completed"
                    + " AND s.verdict <> :systemError AND s.deletedAt IS NULL";

    @Query(
            "SELECT s FROM Submission s WHERE s.problemId = :problemId AND s.contestId IS NULL"
                    + " AND s.status = :completed AND s.deletedAt IS NULL")
    List<Submission> findPracticeToRejudge(
            @Param("problemId") UUID problemId, @Param("completed") SubmissionStatus completed);

    @Query("SELECT COUNT(s)" + JUDGED_OF_PROBLEM)
    long countJudged(
            @Param("problemId") UUID problemId,
            @Param("completed") SubmissionStatus completed,
            @Param("systemError") Verdict systemError);

    @Query("SELECT COUNT(s)" + JUDGED_OF_PROBLEM + " AND s.verdict = :accepted")
    long countAccepted(
            @Param("problemId") UUID problemId,
            @Param("completed") SubmissionStatus completed,
            @Param("systemError") Verdict systemError,
            @Param("accepted") Verdict accepted);

    @Query(
            "SELECT new com.sqljudge.submission.repository.UserJudgingRow(s.userId,"
                    + " MIN(CASE WHEN s.verdict = :accepted THEN s.judgedAt ELSE NULL END))"
                    + JUDGED_OF_PROBLEM
                    + " GROUP BY s.userId")
    List<UserJudgingRow> findUserJudgings(
            @Param("problemId") UUID problemId,
            @Param("completed") SubmissionStatus completed,
            @Param("systemError") Verdict systemError,
            @Param("accepted") Verdict accepted);

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

    @Query(
            value =
                    """
                    SELECT COUNT(DISTINCT s.problem_id, s.dialect, COALESCE(s.code_hash, s.id))
                    FROM submissions s
                    LEFT JOIN (
                        SELECT problem_id, MIN(created_at) AS first_accepted_at
                        FROM submissions
                        WHERE user_id = UUID_TO_BIN(:userId) AND verdict = 'AC'
                          AND deleted_at IS NULL
                        GROUP BY problem_id) solved ON solved.problem_id = s.problem_id
                    WHERE s.user_id = UUID_TO_BIN(:userId) AND s.status = 'COMPLETED'
                      AND s.verdict <> 'SE' AND s.deleted_at IS NULL
                      AND (solved.first_accepted_at IS NULL
                           OR s.created_at <= solved.first_accepted_at)
                    """,
            nativeQuery = true)
    long countOwnAttempts(@Param("userId") String userId);

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

    @Query(
            "SELECT new com.sqljudge.submission.dto.response.RecentSubmissionResponse(s.id,"
                    + " s.status, s.verdict, s.dialect, s.runtimeMs, s.createdAt, s.problemId,"
                    + " s.problemNumber, s.problemSlug, s.problemTitle) FROM Submission s"
                    + " WHERE s.userId = :userId AND s.deletedAt IS NULL AND s.contestId IS NULL"
                    + " ORDER BY s.createdAt DESC, s.id DESC")
    List<RecentSubmissionResponse> findRecent(@Param("userId") UUID userId, Limit limit);

    String HISTORY_SELECT =
            "SELECT new com.sqljudge.submission.dto.response.SubmissionSummaryResponse("
                    + "s.id, s.status, s.verdict, s.dialect, s.runtimeMs, s.createdAt)"
                    + " FROM Submission s";

    String PUBLIC_SELECT =
            "SELECT new com.sqljudge.submission.dto.response.PublicSubmissionResponse(s.id,"
                    + " s.verdict, s.problemNumber, s.problemSlug, s.problemTitle, s.dialect,"
                    + " s.createdAt) FROM Submission s WHERE s.userId = :userId"
                    + " AND s.contestId IS NULL AND s.deletedAt IS NULL"
                    + " AND s.status = com.sqljudge.submission.entity.SubmissionStatus.COMPLETED"
                    + " AND s.verdict <> com.sqljudge.submission.entity.Verdict.SE";

    @Query(PUBLIC_SELECT + " ORDER BY s.id DESC")
    List<PublicSubmissionResponse> findPublic(@Param("userId") UUID userId, Limit limit);

    @Query(PUBLIC_SELECT + " AND s.id < :beforeId ORDER BY s.id DESC")
    List<PublicSubmissionResponse> findPublicBefore(
            @Param("userId") UUID userId, @Param("beforeId") UUID beforeId, Limit limit);
}
