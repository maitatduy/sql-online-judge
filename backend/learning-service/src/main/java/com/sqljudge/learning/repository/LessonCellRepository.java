package com.sqljudge.learning.repository;

import com.sqljudge.learning.dto.response.LessonCellResponse;
import com.sqljudge.learning.entity.LessonCell;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonCellRepository extends JpaRepository<LessonCell, UUID> {

    @Query(
            """
            SELECT new com.sqljudge.learning.dto.response.LessonCellResponse(
                c.cellKey, c.starterSql, c.dialect)
            FROM LessonCell c
            WHERE c.lesson.id = :lessonId
            ORDER BY c.position
            """)
    List<LessonCellResponse> findByLessonId(@Param("lessonId") UUID lessonId);

    @Query(
            """
            SELECT cell FROM LessonCell cell
            JOIN cell.lesson l JOIN l.chapter ch JOIN ch.revision r JOIN r.course c
            WHERE cell.cellKey = :cellKey
              AND c.status = com.sqljudge.learning.entity.CourseStatus.PUBLISHED
              AND c.deletedAt IS NULL
              AND r.id = c.publishedRevisionId
            """)
    Optional<LessonCell> findPublishedByCellKey(@Param("cellKey") UUID cellKey);

    @Modifying
    @Query("DELETE FROM LessonCell c WHERE c.lesson.id = :lessonId")
    void deleteByLessonId(@Param("lessonId") UUID lessonId);

    @Query("SELECT c FROM LessonCell c WHERE c.lesson.id = :lessonId ORDER BY c.position")
    List<LessonCell> findStudioCells(@Param("lessonId") UUID lessonId);

    @Query("SELECT COALESCE(MAX(c.position), 0) FROM LessonCell c WHERE c.lesson.id = :lessonId")
    int findLastPosition(@Param("lessonId") UUID lessonId);

    @Query(
            """
            SELECT cell FROM LessonCell cell
            JOIN FETCH cell.lesson l JOIN FETCH l.chapter ch
            JOIN FETCH ch.revision r JOIN FETCH r.course
            WHERE cell.id = :cellId
            """)
    Optional<LessonCell> findWithCourse(@Param("cellId") UUID cellId);
}
