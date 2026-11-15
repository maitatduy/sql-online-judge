package com.sqljudge.learning.repository;

import com.sqljudge.learning.entity.LessonProgress;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    boolean existsByUserIdAndLessonKey(UUID userId, UUID lessonKey);

    @Query(
            """
            SELECT p.lessonKey FROM LessonProgress p
            WHERE p.userId = :userId AND p.courseId = :courseId
            """)
    Set<UUID> findCompletedLessonKeys(
            @Param("userId") UUID userId, @Param("courseId") UUID courseId);
}
