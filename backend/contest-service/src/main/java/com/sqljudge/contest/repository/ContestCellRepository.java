package com.sqljudge.contest.repository;

import com.sqljudge.contest.entity.ContestCell;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestCellRepository extends JpaRepository<ContestCell, UUID> {

    Optional<ContestCell> findByContestIdAndUserIdAndProblemId(
            UUID contestId, UUID userId, UUID problemId);
}
