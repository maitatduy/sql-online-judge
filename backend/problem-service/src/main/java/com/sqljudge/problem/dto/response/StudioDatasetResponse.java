package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.dto.response.ProblemDetailResponse.SchemaTableResponse;
import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.service.DatasetSchemas;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudioDatasetResponse(
        UUID id,
        String slug,
        String name,
        String description,
        List<VersionResponse> versions,
        SchemaResponse schema,
        Instant updatedAt) {

    public StudioDatasetResponse {
        versions = List.copyOf(versions);
    }

    public static StudioDatasetResponse from(Dataset dataset) {
        DatasetVersion latest = dataset.latestVersion();
        return new StudioDatasetResponse(
                dataset.getId(),
                dataset.getSlug(),
                dataset.getName(),
                dataset.getDescription(),
                dataset.getVersions().stream()
                        .map(
                                version ->
                                        new VersionResponse(
                                                version.getNumber(), version.getFrozenAt()))
                        .toList(),
                new SchemaResponse(
                        latest.getNumber(), latest.isDraft(), DatasetSchemas.tablesOf(latest)),
                dataset.getUpdatedAt());
    }

    public record VersionResponse(int number, Instant frozenAt) {}

    public record SchemaResponse(
            int versionNumber, boolean isDraft, List<SchemaTableResponse> tables) {

        public SchemaResponse {
            tables = List.copyOf(tables);
        }
    }
}
