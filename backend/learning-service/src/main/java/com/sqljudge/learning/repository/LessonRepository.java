package com.sqljudge.learning.repository;

import com.sqljudge.learning.entity.Lesson;
import java.util.List;
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
}
