package com.sqljudge.learning.repository;

import com.sqljudge.learning.entity.LessonRun;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRunRepository extends JpaRepository<LessonRun, UUID> {

    Optional<LessonRun> findByIdAndUserId(UUID id, UUID userId);
}
