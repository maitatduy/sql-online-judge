package com.sqljudge.learning.repository;

import com.sqljudge.learning.dto.response.CourseSummaryResponse;
import com.sqljudge.learning.entity.Course;
import com.sqljudge.learning.entity.Difficulty;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    @Query(
            value =
                    """
                    SELECT new com.sqljudge.learning.dto.response.CourseSummaryResponse(
                        c.id, c.slug, r.title, r.summary, r.difficulty, r.lessonCount,
                        r.estimatedMinutes)
                    FROM Course c JOIN CourseRevision r ON r.id = c.publishedRevisionId
                    WHERE c.status = com.sqljudge.learning.entity.CourseStatus.PUBLISHED
                      AND c.deletedAt IS NULL
                      AND (:difficulty IS NULL OR r.difficulty = :difficulty)
                    ORDER BY r.publishedAt DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(c)
                    FROM Course c JOIN CourseRevision r ON r.id = c.publishedRevisionId
                    WHERE c.status = com.sqljudge.learning.entity.CourseStatus.PUBLISHED
                      AND c.deletedAt IS NULL
                      AND (:difficulty IS NULL OR r.difficulty = :difficulty)
                    """)
    Page<CourseSummaryResponse> findPublished(
            @Param("difficulty") Difficulty difficulty, Pageable pageable);

    boolean existsBySlug(String slug);

    Optional<Course> findByIdAndDeletedAtIsNull(UUID id);

    List<Course> findTop100ByAuthorIdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID authorId);
}
