package com.sqljudge.contest.repository;

import com.sqljudge.contest.entity.ContestRegistration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestRegistrationRepository extends JpaRepository<ContestRegistration, UUID> {

    Optional<ContestRegistration> findByContestIdAndUserId(UUID contestId, UUID userId);

    long countByContestId(UUID contestId);

    boolean existsByContestIdAndUserId(UUID contestId, UUID userId);

    @Query(
            "SELECT new com.sqljudge.contest.repository.ContestCount(r.contest.id, COUNT(r))"
                    + " FROM ContestRegistration r WHERE r.contest.id IN :contestIds"
                    + " GROUP BY r.contest.id")
    List<ContestCount> countByContestIds(@Param("contestIds") Collection<UUID> contestIds);

    @Query(
            "SELECT r.contest.id FROM ContestRegistration r"
                    + " WHERE r.userId = :userId AND r.contest.id IN :contestIds")
    List<UUID> findRegisteredContestIds(
            @Param("userId") UUID userId, @Param("contestIds") Collection<UUID> contestIds);
}
