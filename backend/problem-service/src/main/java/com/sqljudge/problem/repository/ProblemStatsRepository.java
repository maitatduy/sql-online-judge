package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.ProblemStats;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProblemStatsRepository extends JpaRepository<ProblemStats, UUID> {

    Optional<ProblemStats> findByProblemId(UUID problemId);

    @Modifying
    @Query(
            """
            UPDATE ProblemStats s
            SET s.submissionCount = s.submissionCount + 1,
                s.acceptedCount = s.acceptedCount + :accepted,
                s.solvedUserCount = s.solvedUserCount + :solved,
                s.version = s.version + 1,
                s.updatedAt = :now
            WHERE s.problemId = :problemId
            """)
    int recordSubmission(
            @Param("problemId") UUID problemId,
            @Param("accepted") long accepted,
            @Param("solved") long solved,
            @Param("now") Instant now);
}
