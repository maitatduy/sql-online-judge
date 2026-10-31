package com.sqljudge.problem.repository;

import java.time.Instant;
import java.util.UUID;

public record DatasetSummaryRow(
        UUID id,
        String slug,
        String name,
        String description,
        int latestVersionNumber,
        long draftCount,
        Instant updatedAt) {}
