package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.dto.response.ProblemDetailResponse.SchemaTableResponse;
import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.entity.DatasetTable;
import com.sqljudge.problem.entity.DatasetVariant;
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
        List<VariantResponse> variants,
        Instant updatedAt) {

    public StudioDatasetResponse {
        versions = List.copyOf(versions);
        variants = List.copyOf(variants);
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
                latest.getVariants().stream().map(variant -> toVariant(variant, latest)).toList(),
                dataset.getUpdatedAt());
    }

    private static VariantResponse toVariant(DatasetVariant variant, DatasetVersion version) {
        return new VariantResponse(
                variant.getId(),
                variant.getName(),
                variant.isPublic(),
                version.getTables().stream()
                        .map(DatasetTable::getName)
                        .flatMap(tableName -> variant.findTable(tableName).stream())
                        .map(
                                table ->
                                        new VariantTableResponse(
                                                table.getTableName(), table.getRowCount()))
                        .toList());
    }

    public record VersionResponse(int number, Instant frozenAt) {}

    public record SchemaResponse(
            int versionNumber, boolean isDraft, List<SchemaTableResponse> tables) {

        public SchemaResponse {
            tables = List.copyOf(tables);
        }
    }

    public record VariantResponse(
            UUID id, String name, boolean isPublic, List<VariantTableResponse> tables) {

        public VariantResponse {
            tables = List.copyOf(tables);
        }
    }

    public record VariantTableResponse(String tableName, int rowCount) {}
}
