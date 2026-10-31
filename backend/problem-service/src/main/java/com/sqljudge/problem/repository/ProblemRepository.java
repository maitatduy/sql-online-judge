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

    Optional<Problem> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<Problem> findBySlugAndStatusAndVisibilityAndDeletedAtIsNull(
            String slug, ProblemStatus status, ProblemVisibility visibility);

    @Query("SELECT p.slug FROM Problem p WHERE p.slug = :base OR p.slug LIKE :prefix")
    List<String> findSlugsLike(@Param("base") String base, @Param("prefix") String prefix);

    @Query(
            """
            SELECT p FROM Problem p
            WHERE p.authorId = :authorId AND p.deletedAt IS NULL
              AND (:status IS NULL OR p.status = :status)
              AND (:titlePattern IS NULL OR LOWER(p.title) LIKE :titlePattern ESCAPE '!')
            ORDER BY p.updatedAt DESC
            """)
    Page<Problem> findOwned(
            @Param("authorId") UUID authorId,
            @Param("status") ProblemStatus status,
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
            "SELECT new com.sqljudge.problem.repository.ProblemTagRow(p.id, t.slug, t.name)"
                    + " FROM Problem p JOIN p.tags t WHERE p.id IN :problemIds ORDER BY t.name")
    List<ProblemTagRow> findTagsOf(@Param("problemIds") Collection<UUID> problemIds);
}
