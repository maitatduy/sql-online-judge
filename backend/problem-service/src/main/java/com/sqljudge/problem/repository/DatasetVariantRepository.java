package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.DatasetVariant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DatasetVariantRepository extends JpaRepository<DatasetVariant, UUID> {

    @Query(
            """
            SELECT variant FROM DatasetVariant variant
            JOIN FETCH variant.version version
            JOIN FETCH version.dataset dataset
            WHERE variant.id = :variantId AND dataset.deletedAt IS NULL
            """)
    Optional<DatasetVariant> findLiveWithDataset(@Param("variantId") UUID variantId);
}
