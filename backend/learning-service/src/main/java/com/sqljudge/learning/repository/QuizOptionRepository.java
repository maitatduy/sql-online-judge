package com.sqljudge.learning.repository;

import com.sqljudge.learning.entity.QuizOption;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizOptionRepository extends JpaRepository<QuizOption, UUID> {

    @Query(
            """
            SELECT new com.sqljudge.learning.repository.QuizOptionRow(
                q.questionKey, q.prompt, q.multiple, o.optionKey, o.label, o.correct)
            FROM QuizOption o JOIN o.question q
            WHERE q.lesson.id = :lessonId
            ORDER BY q.position, o.position
            """)
    List<QuizOptionRow> findByLessonId(@Param("lessonId") UUID lessonId);

    boolean existsByQuestionLessonId(UUID lessonId);

    @Modifying
    @Query(
            "DELETE FROM QuizOption o WHERE o.question.id IN"
                    + " (SELECT q.id FROM QuizQuestion q WHERE q.lesson.id = :lessonId)")
    void deleteByLessonId(@Param("lessonId") UUID lessonId);

    @Modifying
    @Query("DELETE FROM QuizQuestion q WHERE q.lesson.id = :lessonId")
    void deleteQuestionsByLessonId(@Param("lessonId") UUID lessonId);
}
