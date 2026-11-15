package com.sqljudge.learning.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "lesson_progress",
        indexes = {
            @Index(
                    name = "uk_lesson_progress_user_id_lesson_key",
                    columnList = "user_id, lesson_key",
                    unique = true),
            @Index(
                    name = "idx_lesson_progress_user_id_course_id",
                    columnList = "user_id, course_id"),
            @Index(
                    name = "idx_lesson_progress_user_id_completed_at",
                    columnList = "user_id, completed_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class LessonProgress extends BaseEntity {

    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "lesson_key", nullable = false, columnDefinition = "BINARY(16)")
    private UUID lessonKey;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    public static LessonProgress completed(
            UUID courseId, UUID userId, UUID lessonKey, Instant completedAt) {
        LessonProgress progress = new LessonProgress();
        progress.setCourseId(courseId);
        progress.setUserId(userId);
        progress.setLessonKey(lessonKey);
        progress.setCompletedAt(completedAt);
        return progress;
    }
}
