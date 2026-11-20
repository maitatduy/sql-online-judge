package com.sqljudge.learning.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record DatasetVariantInfo(
        UUID variantId,
        UUID versionId,
        String datasetName,
        String variantName,
        @JsonProperty("isPublic") boolean isPublic,
        @JsonProperty("isFrozen") boolean isFrozen,
        String checksum) {}
