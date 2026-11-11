package com.sqljudge.submission.repository;

import com.sqljudge.submission.entity.CodeRun;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeRunRepository extends JpaRepository<CodeRun, UUID> {

    Optional<CodeRun> findByIdAndUserId(UUID id, UUID userId);
}
