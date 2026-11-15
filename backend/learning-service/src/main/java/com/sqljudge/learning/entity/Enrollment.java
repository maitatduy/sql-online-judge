package com.sqljudge.learning.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "enrollments",
        indexes = {
            @Index(
                    name = "uk_enrollments_course_id_user_id",
                    columnList = "course_id, user_id",
                    unique = true),
            @Index(name = "idx_enrollments_user_id_created_at", columnList = "user_id, created_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class Enrollment extends BaseEntity {

    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    public static Enrollment of(UUID courseId, UUID userId) {
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setUserId(userId);
        return enrollment;
    }
}
