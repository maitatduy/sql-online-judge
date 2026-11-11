package com.sqljudge.submission.repository;

import com.sqljudge.submission.entity.Submission;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Optional<Submission> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
