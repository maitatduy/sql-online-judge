package com.sqljudge.learning.repository;

import com.sqljudge.learning.entity.CourseRevision;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRevisionRepository extends JpaRepository<CourseRevision, UUID> {

    @Query(
            """
            SELECT r FROM CourseRevision r JOIN FETCH r.course c
            WHERE c.slug = :slug
              AND c.status = com.sqljudge.learning.entity.CourseStatus.PUBLISHED
              AND c.deletedAt IS NULL
              AND r.id = c.publishedRevisionId
            """)
    Optional<CourseRevision> findPublishedBySlug(@Param("slug") String slug);

    @Query(
            """
            SELECT r FROM CourseRevision r JOIN FETCH r.course c
            WHERE c.id = :courseId
              AND c.status = com.sqljudge.learning.entity.CourseStatus.PUBLISHED
              AND c.deletedAt IS NULL
              AND r.id = c.publishedRevisionId
            """)
    Optional<CourseRevision> findPublishedByCourseId(@Param("courseId") UUID courseId);
}
