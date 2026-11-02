package com.sqljudge.problem.dto.response;

import java.util.List;
import java.util.UUID;

public record StudioRunOutcome(
        Rejection rejection, String systemError, List<VariantResult> variants) {

    public StudioRunOutcome {
        variants = variants == null ? List.of() : List.copyOf(variants);
    }

    public record Rejection(String verdict, String message) {}

    public record VariantResult(
            UUID variantId,
            String variantName,
            boolean isPublic,
            String status,
            List<String> columns,
            List<List<Object>> rows,
            int rowCount,
            boolean truncated,
            long runtimeMs,
            String message) {}
}
