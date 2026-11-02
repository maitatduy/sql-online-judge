package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.DatasetVersion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetVersionRepository extends JpaRepository<DatasetVersion, UUID> {

    Optional<DatasetVersion> findByIdAndFrozenAtIsNotNull(UUID id);
}
