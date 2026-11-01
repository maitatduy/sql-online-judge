package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Dataset;
import java.time.Instant;
import java.util.UUID;

public record StudioDatasetSummaryResponse(
        UUID id, String slug, String name, String description, Instant updatedAt) {

    public static StudioDatasetSummaryResponse from(Dataset dataset) {
        return new StudioDatasetSummaryResponse(
                dataset.getId(),
                dataset.getSlug(),
                dataset.getName(),
                dataset.getDescription(),
                dataset.getUpdatedAt());
    }
}
