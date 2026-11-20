package com.sqljudge.learning.repository;

import com.sqljudge.learning.entity.QuizQuestion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    @Query("SELECT q FROM QuizQuestion q WHERE q.lesson.id = :lessonId ORDER BY q.position")
    List<QuizQuestion> findStudioQuestions(@Param("lessonId") UUID lessonId);

    @Query("SELECT COALESCE(MAX(q.position), 0) FROM QuizQuestion q WHERE q.lesson.id = :lessonId")
    int findLastPosition(@Param("lessonId") UUID lessonId);

    @Query(
            """
            SELECT q FROM QuizQuestion q
            JOIN FETCH q.lesson l JOIN FETCH l.chapter ch
            JOIN FETCH ch.revision r JOIN FETCH r.course
            WHERE q.id = :questionId
            """)
    Optional<QuizQuestion> findWithCourse(@Param("questionId") UUID questionId);

    @Modifying
    @Query("DELETE FROM QuizQuestion q WHERE q.lesson.id = :lessonId")
    void deleteByLessonId(@Param("lessonId") UUID lessonId);
}
