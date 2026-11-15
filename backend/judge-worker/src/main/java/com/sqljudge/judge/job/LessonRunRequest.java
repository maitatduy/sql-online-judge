package com.sqljudge.judge.job;

import com.sqljudge.judge.sandbox.Dialect;
import java.util.UUID;

public record LessonRunRequest(
        UUID runId,
        UUID datasetVersionId,
        UUID datasetVariantId,
        String datasetChecksum,
        Dialect dialect,
        String sql,
        int timeLimitMs) {}
