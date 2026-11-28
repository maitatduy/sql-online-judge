package com.sqljudge.contest.repository;

import com.sqljudge.contest.entity.ContestAttempt;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestAttemptRepository extends JpaRepository<ContestAttempt, UUID> {

    boolean existsBySubmissionId(UUID submissionId);

    List<ContestAttempt> findByContestIdAndUserIdAndProblemId(
            UUID contestId, UUID userId, UUID problemId);

    @Query(
            "SELECT new com.sqljudge.contest.repository.ContestCount(a.contestId,"
                    + " COUNT(DISTINCT a.userId)) FROM ContestAttempt a"
                    + " WHERE a.contestId IN :contestIds GROUP BY a.contestId")
    List<ContestCount> countParticipantsByContestIds(
            @Param("contestIds") Collection<UUID> contestIds);
}
