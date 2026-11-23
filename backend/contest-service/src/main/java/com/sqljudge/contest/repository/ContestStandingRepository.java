package com.sqljudge.contest.repository;

import com.sqljudge.contest.entity.ContestStanding;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestStandingRepository extends JpaRepository<ContestStanding, UUID> {}
