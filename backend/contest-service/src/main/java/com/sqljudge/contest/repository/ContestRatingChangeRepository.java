package com.sqljudge.contest.repository;

import com.sqljudge.contest.entity.ContestRatingChange;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestRatingChangeRepository extends JpaRepository<ContestRatingChange, UUID> {

    List<ContestRatingChange> findByContestIdOrderByRankAscUsernameAsc(UUID contestId);
}
