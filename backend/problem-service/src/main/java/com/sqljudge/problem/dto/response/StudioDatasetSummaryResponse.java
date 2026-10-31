package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.repository.DatasetSummaryRow;
import java.time.Instant;
import java.util.UUID;

public record StudioDatasetSummaryResponse(
        UUID id,
        String slug,
        String name,
        String description,
        int latestVersionNumber,
        boolean hasDraft,
        Instant updatedAt) {

    public static StudioDatasetSummaryResponse from(DatasetSummaryRow row) {
        return new StudioDatasetSummaryResponse(
                row.id(),
                row.slug(),
                row.name(),
                row.description(),
                row.latestVersionNumber(),
                row.draftCount() > 0,
                row.updatedAt());
    }
}
