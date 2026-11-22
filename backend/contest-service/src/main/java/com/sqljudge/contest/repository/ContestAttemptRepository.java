package com.sqljudge.contest.repository;

import com.sqljudge.contest.entity.ContestAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestAttemptRepository extends JpaRepository<ContestAttempt, UUID> {

    boolean existsBySubmissionId(UUID submissionId);

    List<ContestAttempt> findByContestIdAndUserIdAndProblemId(
            UUID contestId, UUID userId, UUID problemId);
}
