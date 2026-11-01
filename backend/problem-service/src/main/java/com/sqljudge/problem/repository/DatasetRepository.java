package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.Dataset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DatasetRepository extends JpaRepository<Dataset, UUID> {

    String OWNED_DATASETS =
            """
            FROM Dataset d
            WHERE d.ownerId = :ownerId AND d.deletedAt IS NULL
              AND (:namePattern IS NULL OR LOWER(d.name) LIKE :namePattern ESCAPE '!')
            """;

    Optional<Dataset> findBySlug(String slug);

    Optional<Dataset> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT d.slug FROM Dataset d WHERE d.slug = :base OR d.slug LIKE :prefix")
    List<String> findSlugsLike(@Param("base") String base, @Param("prefix") String prefix);

    @Query("SELECT d " + OWNED_DATASETS)
    Page<Dataset> findOwned(
            @Param("ownerId") UUID ownerId,
            @Param("namePattern") String namePattern,
            Pageable pageable);
}
