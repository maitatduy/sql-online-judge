package com.sqljudge.learning.repository;

import com.sqljudge.learning.entity.CourseReview;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseReviewRepository extends JpaRepository<CourseReview, UUID> {

    Optional<CourseReview> findFirstByCourseIdOrderByCreatedAtDesc(UUID courseId);
}
