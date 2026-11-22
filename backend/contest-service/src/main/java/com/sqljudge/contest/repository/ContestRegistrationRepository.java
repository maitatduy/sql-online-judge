package com.sqljudge.contest.repository;

import com.sqljudge.contest.entity.ContestRegistration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestRegistrationRepository extends JpaRepository<ContestRegistration, UUID> {

    Optional<ContestRegistration> findByContestIdAndUserId(UUID contestId, UUID userId);

    long countByContestId(UUID contestId);
}
