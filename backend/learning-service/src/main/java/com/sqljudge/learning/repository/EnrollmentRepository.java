package com.sqljudge.learning.repository;

import com.sqljudge.learning.entity.Enrollment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    boolean existsByCourseIdAndUserId(UUID courseId, UUID userId);
}
