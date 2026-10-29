package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.UserProblemProgress;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProblemProgressRepository extends JpaRepository<UserProblemProgress, UUID> {}
