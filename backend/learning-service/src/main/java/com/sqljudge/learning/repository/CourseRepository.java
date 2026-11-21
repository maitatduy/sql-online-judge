package com.sqljudge.learning.repository;

import com.sqljudge.learning.dto.response.AdminCourseItem;
import com.sqljudge.learning.dto.response.CourseSummaryResponse;
import com.sqljudge.learning.entity.Course;
import com.sqljudge.learning.entity.CourseStatus;
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

    @Query(
            value =
                    """
                    SELECT new com.sqljudge.learning.dto.response.AdminCourseItem(
                        c.id, c.slug, w.title, w.difficulty, c.status, w.status, w.lessonCount,
                        c.authorId, c.updatedAt, p.publishedAt)
                    FROM Course c
                    JOIN CourseRevision w
                        ON w.id = COALESCE(c.draftRevisionId, c.publishedRevisionId)
                    LEFT JOIN CourseRevision p ON p.id = c.publishedRevisionId
                    WHERE c.deletedAt IS NULL AND (:status IS NULL OR c.status = :status)
                    ORDER BY c.updatedAt DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(c) FROM Course c
                    WHERE c.deletedAt IS NULL AND (:status IS NULL OR c.status = :status)
                    """)
    Page<AdminCourseItem> findCatalog(@Param("status") CourseStatus status, Pageable pageable);
}
