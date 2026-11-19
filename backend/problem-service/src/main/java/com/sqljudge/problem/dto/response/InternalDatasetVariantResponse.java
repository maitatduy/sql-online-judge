package com.sqljudge.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.problem.entity.DatasetVariant;
import com.sqljudge.problem.entity.DatasetVersion;
import java.util.UUID;

public record InternalDatasetVariantResponse(
        UUID variantId,
        UUID versionId,
        String datasetName,
        String variantName,
        @JsonProperty("isPublic") boolean isPublic,
        @JsonProperty("isFrozen") boolean isFrozen,
        String checksum) {

    public static InternalDatasetVariantResponse of(DatasetVariant variant) {
        DatasetVersion version = variant.getVersion();
        return new InternalDatasetVariantResponse(
                variant.getId(),
                version.getId(),
                version.getDataset().getName(),
                variant.getName(),
                variant.isPublic(),
                version.getFrozenAt() != null,
                version.getChecksum());
    }
}
