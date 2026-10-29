package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.ProblemStats;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemStatsRepository extends JpaRepository<ProblemStats, UUID> {

    Optional<ProblemStats> findByProblemId(UUID problemId);
}
