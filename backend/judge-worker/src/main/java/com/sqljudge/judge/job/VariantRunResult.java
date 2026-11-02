package com.sqljudge.judge.job;

import java.util.List;
import java.util.UUID;

public record VariantRunResult(
        UUID variantId,
        String variantName,
        boolean isPublic,
        RunStatus status,
        List<String> columns,
        List<List<Object>> rows,
        int rowCount,
        boolean truncated,
        long runtimeMs,
        String message) {

    public VariantRunResult {
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
    }
}
