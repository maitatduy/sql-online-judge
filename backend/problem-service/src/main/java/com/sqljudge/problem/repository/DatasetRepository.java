package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.Dataset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<Dataset, UUID> {

    Optional<Dataset> findBySlug(String slug);
}
