package com.sqljudge.learning.repository;

import com.sqljudge.learning.entity.Lesson;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    @Query(
            """
            SELECT new com.sqljudge.learning.repository.LessonOutlineRow(
                ch.id, ch.title, l.lessonKey, l.slug, l.title)
            FROM Chapter ch LEFT JOIN Lesson l ON l.chapter = ch
            WHERE ch.revision.id = :revisionId
            ORDER BY ch.position, l.position
            """)
    List<LessonOutlineRow> findOutline(@Param("revisionId") UUID revisionId);

    @Query(
            """
            SELECT new com.sqljudge.learning.repository.PublishedLessonRow(
                l.id, l.lessonKey, l.slug, l.title, l.body, ch.title, c.slug, r.title)
            FROM Lesson l JOIN l.chapter ch JOIN ch.revision r JOIN r.course c
            WHERE c.slug = :courseSlug
              AND l.slug = :lessonSlug
              AND c.status = com.sqljudge.learning.entity.CourseStatus.PUBLISHED
              AND c.deletedAt IS NULL
              AND r.id = c.publishedRevisionId
            """)
    Optional<PublishedLessonRow> findPublished(
            @Param("courseSlug") String courseSlug, @Param("lessonSlug") String lessonSlug);

    boolean existsByChapterId(UUID chapterId);

    @Query(
            """
            SELECT new com.sqljudge.learning.repository.StudioLessonRow(
                l.chapter.id, l.id, l.slug, l.title)
            FROM Lesson l
            WHERE l.chapter.revision.id = :revisionId
            ORDER BY l.chapter.position, l.position
            """)
    List<StudioLessonRow> findStudioOutline(@Param("revisionId") UUID revisionId);
}
