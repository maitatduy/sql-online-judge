package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemProgressStatus;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    @Query(
            "SELECT p.difficulty AS difficulty, COUNT(p) AS total FROM Problem p"
                    + " WHERE p.status = :status AND p.visibility = :visibility"
                    + " AND p.deletedAt IS NULL GROUP BY p.difficulty")
    List<DifficultyCount> countPublishedByDifficulty(
            @Param("status") ProblemStatus status,
            @Param("visibility") ProblemVisibility visibility);

    @Query(
            "SELECT p.difficulty AS difficulty, COUNT(p) AS total"
                    + " FROM UserProblemProgress g JOIN Problem p ON p.id = g.problemId"
                    + " WHERE g.userId = :userId AND g.status = :solved AND p.deletedAt IS NULL"
                    + " GROUP BY p.difficulty")
    List<DifficultyCount> countSolvedByDifficulty(
            @Param("userId") UUID userId, @Param("solved") ProblemProgressStatus solved);

    @Query(
            "SELECT t.slug AS slug, t.name AS name, COUNT(p) AS total FROM Problem p JOIN p.tags t"
                    + " WHERE p.status = :status AND p.visibility = :visibility"
                    + " AND p.deletedAt IS NULL GROUP BY t.slug, t.name")
    List<TagTotalRow> countPublishedByTag(
            @Param("status") ProblemStatus status,
            @Param("visibility") ProblemVisibility visibility);

    @Query(
            "SELECT t.slug AS slug, COUNT(p) AS total"
                    + " FROM UserProblemProgress g JOIN Problem p ON p.id = g.problemId"
                    + " JOIN p.tags t"
                    + " WHERE g.userId = :userId AND g.status = :solved"
                    + " AND p.status = :status AND p.visibility = :visibility"
                    + " AND p.deletedAt IS NULL GROUP BY t.slug")
    List<TagUsageRow> countSolvedByTag(
            @Param("userId") UUID userId,
            @Param("solved") ProblemProgressStatus solved,
            @Param("status") ProblemStatus status,
            @Param("visibility") ProblemVisibility visibility);

    @Query(
            "SELECT p.id FROM Problem p"
                    + " LEFT JOIN UserProblemProgress up"
                    + " ON up.problemId = p.id AND up.userId = :userId AND up.status = :solved"
                    + " WHERE p.status = :status AND p.visibility = :visibility"
                    + " AND p.deletedAt IS NULL AND up.id IS NULL ORDER BY p.number")
    List<UUID> findUnsolvedIds(
            @Param("userId") UUID userId,
            @Param("solved") ProblemProgressStatus solved,
            @Param("status") ProblemStatus status,
            @Param("visibility") ProblemVisibility visibility);

    @Query(
            "SELECT new com.sqljudge.problem.repository.ProblemSummaryRow(p.id, p.number,"
                    + " p.slug, p.title, p.difficulty, s.submissionCount, s.acceptedCount,"
                    + " s.solvedUserCount, up.status) FROM Problem p"
                    + " LEFT JOIN ProblemStats s ON s.problemId = p.id"
                    + " LEFT JOIN UserProblemProgress up"
                    + " ON up.problemId = p.id AND up.userId = :userId"
                    + " WHERE p.id IN :problemIds")
    List<ProblemSummaryRow> findSummariesOf(
            @Param("problemIds") Collection<UUID> problemIds, @Param("userId") UUID userId);

    String ADMIN_PROBLEMS =
            """
            FROM Problem p
            LEFT JOIN ProblemStats s ON s.problemId = p.id
            WHERE p.status = :status AND p.deletedAt IS NULL
              AND (:visibility IS NULL OR p.visibility = :visibility)
              AND (:difficulty IS NULL OR p.difficulty = :difficulty)
              AND (:tag IS NULL OR EXISTS (
                    SELECT 1 FROM Problem tagged JOIN tagged.tags t
                    WHERE tagged = p AND t.slug = :tag))
              AND (:titlePattern IS NULL OR LOWER(p.title) LIKE :titlePattern ESCAPE '!'
                    OR p.number = :number)
            """;

    String LISTED_PROBLEMS =
            """
            FROM Problem p
            LEFT JOIN ProblemStats s ON s.problemId = p.id
            LEFT JOIN UserProblemProgress up ON up.problemId = p.id AND up.userId = :userId
            WHERE p.status = :status AND p.visibility = :visibility AND p.deletedAt IS NULL
              AND (:difficulty IS NULL OR p.difficulty = :difficulty)
              AND (:tag IS NULL OR EXISTS (
                    SELECT 1 FROM Problem tagged JOIN tagged.tags t
                    WHERE tagged = p AND t.slug = :tag))
              AND (:titlePattern IS NULL OR LOWER(p.title) LIKE :titlePattern ESCAPE '!'
                    OR p.number = :number)
              AND (:notStarted = FALSE OR up.id IS NULL)
              AND (:progress IS NULL OR up.status = :progress)
            """;

    Optional<Problem> findBySlug(String slug);

    @Query("SELECT COALESCE(MAX(p.number), 0) FROM Problem p")
    int findMaxNumber();

    @Query(
            "SELECT COUNT(DISTINCT p) FROM Problem p JOIN p.revisions r"
                    + " WHERE r.datasetVersion.dataset.id = :datasetId AND p.deletedAt IS NULL")
    long countUsingDataset(@Param("datasetId") UUID datasetId);

    Optional<Problem> findByIdAndDeletedAtIsNull(UUID id);

    Page<Problem> findByStatusAndDeletedAtIsNull(ProblemStatus status, Pageable pageable);

    Page<Problem> findByStatusAndVisibilityAndDeletedAtIsNull(
            ProblemStatus status, ProblemVisibility visibility, Pageable pageable);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<Problem> findBySlugAndStatusAndVisibilityAndDeletedAtIsNull(
            String slug, ProblemStatus status, ProblemVisibility visibility);

    List<Problem> findByIdInAndStatusAndVisibilityAndDeletedAtIsNull(
            Collection<UUID> ids, ProblemStatus status, ProblemVisibility visibility);

    Optional<Problem> findByIdAndStatusAndVisibilityAndDeletedAtIsNull(
            UUID id, ProblemStatus status, ProblemVisibility visibility);

    @Query("SELECT p.slug FROM Problem p WHERE p.slug = :base OR p.slug LIKE :prefix")
    List<String> findSlugsLike(@Param("base") String base, @Param("prefix") String prefix);

    @Query(
            """
            SELECT p FROM Problem p
            WHERE p.authorId = :authorId AND p.deletedAt IS NULL
              AND (:status IS NULL OR p.status = :status)
              AND (:difficulty IS NULL OR p.difficulty = :difficulty)
              AND (:titlePattern IS NULL OR LOWER(p.title) LIKE :titlePattern ESCAPE '!')
            """)
    Page<Problem> findOwned(
            @Param("authorId") UUID authorId,
            @Param("status") ProblemStatus status,
            @Param("difficulty") Difficulty difficulty,
            @Param("titlePattern") String titlePattern,
            Pageable pageable);

    @Query(
            "SELECT p.status AS status, COUNT(p) AS total FROM Problem p"
                    + " WHERE p.authorId = :authorId AND p.deletedAt IS NULL GROUP BY p.status")
    List<StatusCountRow> countOwnedByStatus(@Param("authorId") UUID authorId);

    @Query(
            value =
                    "SELECT new com.sqljudge.problem.repository.ProblemSummaryRow(p.id, p.number,"
                            + " p.slug, p.title, p.difficulty, s.submissionCount, s.acceptedCount,"
                            + " s.solvedUserCount, up.status) "
                            + LISTED_PROBLEMS
                            + " ORDER BY p.number",
            countQuery = "SELECT COUNT(p) " + LISTED_PROBLEMS)
    Page<ProblemSummaryRow> findListed(
            @Param("status") ProblemStatus status,
            @Param("visibility") ProblemVisibility visibility,
            @Param("userId") UUID userId,
            @Param("difficulty") Difficulty difficulty,
            @Param("tag") String tag,
            @Param("titlePattern") String titlePattern,
            @Param("number") Integer number,
            @Param("notStarted") boolean notStarted,
            @Param("progress") ProblemProgressStatus progress,
            Pageable pageable);

    @Query(
            value =
                    "SELECT new com.sqljudge.problem.repository.AdminProblemRow(p.id, p.number,"
                            + " p.slug, p.title, p.status, p.visibility, p.difficulty, p.authorId,"
                            + " p.updatedAt, s.submissionCount, s.acceptedCount) "
                            + ADMIN_PROBLEMS,
            countQuery = "SELECT COUNT(p) " + ADMIN_PROBLEMS)
    Page<AdminProblemRow> findForAdmin(
            @Param("status") ProblemStatus status,
            @Param("visibility") ProblemVisibility visibility,
            @Param("difficulty") Difficulty difficulty,
            @Param("tag") String tag,
            @Param("titlePattern") String titlePattern,
            @Param("number") Integer number,
            Pageable pageable);

    @Query(
            "SELECT p.visibility AS visibility, COUNT(p) AS total FROM Problem p"
                    + " WHERE p.status = :status AND p.deletedAt IS NULL GROUP BY p.visibility")
    List<VisibilityCountRow> countByVisibility(@Param("status") ProblemStatus status);

    @Query(
            "SELECT COUNT(p) FROM Problem p JOIN ProblemStats s ON s.problemId = p.id"
                    + " WHERE p.status = :status AND p.deletedAt IS NULL"
                    + " AND s.submissionCount >= :minSubmissions"
                    + " AND s.acceptedCount * 100 < s.submissionCount * :maxPercent")
    long countLowAcceptance(
            @Param("status") ProblemStatus status,
            @Param("minSubmissions") long minSubmissions,
            @Param("maxPercent") long maxPercent);

    @Query(
            "SELECT new com.sqljudge.problem.repository.ProblemTagRow(p.id, t.slug, t.name)"
                    + " FROM Problem p JOIN p.tags t WHERE p.id IN :problemIds ORDER BY t.name")
    List<ProblemTagRow> findTagsOf(@Param("problemIds") Collection<UUID> problemIds);
}
