package com.sqljudge.contest.repository;

import com.sqljudge.contest.entity.ContestCell;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContestCellRepository extends JpaRepository<ContestCell, UUID> {

    Optional<ContestCell> findByContestIdAndUserIdAndProblemId(
            UUID contestId, UUID userId, UUID problemId);

    List<ContestCell> findByContestIdAndUserId(UUID contestId, UUID userId);

    List<ContestCell> findByContestId(UUID contestId);

    @Query("SELECT DISTINCT c.userId FROM ContestCell c WHERE c.contestId = :contestId")
    List<UUID> findUserIdsByContestId(UUID contestId);
}
