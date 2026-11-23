package com.sqljudge.community.repository;

import com.sqljudge.community.entity.Vote;
import com.sqljudge.community.entity.VoteTarget;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    Optional<Vote> findByTargetTypeAndTargetIdAndUserId(
            VoteTarget targetType, UUID targetId, UUID userId);

    List<Vote> findByTargetTypeAndTargetIdInAndUserId(
            VoteTarget targetType, Collection<UUID> targetIds, UUID userId);

    @Query(
            "SELECT v.targetAuthorId AS userId, SUM(v.value) AS points FROM Vote v"
                    + " WHERE v.updatedAt >= :since GROUP BY v.targetAuthorId"
                    + " HAVING SUM(v.value) > 0 ORDER BY SUM(v.value) DESC")
    List<ContributorRow> findTopContributors(@Param("since") Instant since, Pageable pageable);
}
