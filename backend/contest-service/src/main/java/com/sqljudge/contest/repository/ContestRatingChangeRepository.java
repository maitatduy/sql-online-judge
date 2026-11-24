package com.sqljudge.contest.repository;

import com.sqljudge.contest.dto.response.RatingPointResponse;
import com.sqljudge.contest.entity.ContestRatingChange;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestRatingChangeRepository extends JpaRepository<ContestRatingChange, UUID> {

    List<ContestRatingChange> findByContestIdOrderByRankAscUsernameAsc(UUID contestId);

    @Query(
            "SELECT new com.sqljudge.contest.dto.response.RatingPointResponse(c.slug, c.title,"
                    + " r.newRating, r.rank, c.endAt) FROM ContestRatingChange r"
                    + " JOIN Contest c ON c.id = r.contestId WHERE r.userId = :userId"
                    + " ORDER BY c.endAt ASC, c.id ASC")
    List<RatingPointResponse> findHistory(@Param("userId") UUID userId);
}
