package com.sqljudge.judge.job;

import com.sqljudge.judge.sandbox.Dialect;
import java.util.UUID;

public record StudioRunRequest(
        UUID runId,
        UUID datasetVersionId,
        String datasetChecksum,
        Dialect dialect,
        String sql,
        int timeLimitMs) {}
