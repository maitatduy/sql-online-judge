package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.ProblemRevision;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRevisionRepository extends JpaRepository<ProblemRevision, UUID> {

    @EntityGraph(
            attributePaths = {
                "problem",
                "datasetVersion",
                "testcases",
                "testcases.datasetVariant",
                "testcases.expectedResult"
            })
    Optional<ProblemRevision> findByIdAndFrozenAtIsNotNull(UUID id);
}
