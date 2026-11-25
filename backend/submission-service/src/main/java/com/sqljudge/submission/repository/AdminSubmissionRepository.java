package com.sqljudge.submission.repository;

import com.sqljudge.submission.entity.Dialect;
import com.sqljudge.submission.entity.Submission;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.entity.Verdict;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminSubmissionRepository extends JpaRepository<Submission, UUID> {

    String ROW =
            "SELECT new com.sqljudge.submission.repository.AdminSubmissionRow(s.id, s.userId,"
                    + " s.problemId, s.problemNumber, s.problemSlug, s.problemTitle, s.contestId,"
                    + " s.dialect, s.status, s.verdict, s.runtimeMs, s.createdAt)";

    long countByStatusAndDeletedAtIsNull(SubmissionStatus status);

    @Query(
            "SELECT COUNT(s) FROM Submission s WHERE s.deletedAt IS NULL"
                    + " AND s.status <> com.sqljudge.submission.entity.SubmissionStatus.COMPLETED"
                    + " AND s.updatedAt < :stuckBefore")
    long countStuck(@Param("stuckBefore") Instant stuckBefore);

    long countByVerdictAndJudgedAtGreaterThanEqualAndDeletedAtIsNull(
            Verdict verdict, Instant judgedFrom);

    long countByStatusAndJudgedAtGreaterThanEqualAndDeletedAtIsNull(
            SubmissionStatus status, Instant judgedFrom);

    @Query(
            ROW
                    + " FROM Submission s WHERE s.deletedAt IS NULL AND ((s.status <>"
                    + " com.sqljudge.submission.entity.SubmissionStatus.COMPLETED"
                    + " AND s.updatedAt < :stuckBefore)"
                    + " OR (s.verdict = com.sqljudge.submission.entity.Verdict.SE"
                    + " AND s.judgedAt >= :errorsFrom))"
                    + " ORDER BY s.id DESC")
    List<AdminSubmissionRow> findNeedingAttention(
            @Param("stuckBefore") Instant stuckBefore,
            @Param("errorsFrom") Instant errorsFrom,
            Limit limit);

    String MATCHING =
            " FROM Submission s WHERE s.deletedAt IS NULL"
                    + " AND (:verdict IS NULL OR s.verdict = :verdict)"
                    + " AND (:status IS NULL OR s.status = :status)"
                    + " AND (:dialect IS NULL OR s.dialect = :dialect)"
                    + " AND (:userId IS NULL OR s.userId = :userId)"
                    + " AND (:problemSlug IS NULL OR s.problemSlug = :problemSlug)"
                    + " AND (:practiceOnly = FALSE OR s.contestId IS NULL)"
                    + " AND (:contestOnly = FALSE OR s.contestId IS NOT NULL)";

    @Query(
            value =
                    "SELECT new com.sqljudge.submission.repository.AdminSubmissionRow(s.id,"
                            + " s.userId, s.problemId, s.problemNumber, s.problemSlug,"
                            + " s.problemTitle, s.contestId, s.dialect, s.status, s.verdict,"
                            + " s.runtimeMs, s.createdAt)"
                            + MATCHING
                            + " ORDER BY s.id DESC",
            countQuery = "SELECT COUNT(s)" + MATCHING)
    Page<AdminSubmissionRow> search(
            @Param("verdict") Verdict verdict,
            @Param("status") SubmissionStatus status,
            @Param("dialect") Dialect dialect,
            @Param("userId") UUID userId,
            @Param("problemSlug") String problemSlug,
            @Param("practiceOnly") boolean practiceOnly,
            @Param("contestOnly") boolean contestOnly,
            Pageable pageable);

    default Page<AdminSubmissionRow> search(AdminSubmissionFilter filter, Pageable pageable) {
        return search(
                filter.verdict(),
                filter.status(),
                filter.dialect(),
                filter.userId(),
                filter.problemSlug(),
                filter.source() == SubmissionSource.PRACTICE,
                filter.source() == SubmissionSource.CONTEST,
                pageable);
    }
}
