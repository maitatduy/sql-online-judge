package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.StudioRun;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudioRunRepository extends JpaRepository<StudioRun, UUID> {

    Optional<StudioRun> findByIdAndProblemId(UUID id, UUID problemId);
}
