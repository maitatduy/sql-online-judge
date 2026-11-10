package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.AnswerCheck;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerCheckRepository extends JpaRepository<AnswerCheck, UUID> {

    Optional<AnswerCheck> findByIdAndProblemId(UUID id, UUID problemId);

    Optional<AnswerCheck> findFirstByProblemIdOrderByCreatedAtDescIdDesc(UUID problemId);
}
