package com.sqljudge.judge.consumer;

import com.sqljudge.judge.job.StudioRunRequest;
import com.sqljudge.judge.sandbox.Dialect;
import java.time.Instant;
import java.util.UUID;

public record StudioRunRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID runId,
        UUID datasetVersionId,
        String datasetChecksum,
        Dialect dialect,
        String sql,
        int timeLimitMs) {

    StudioRunRequest toRequest() {
        return new StudioRunRequest(
                runId, datasetVersionId, datasetChecksum, dialect, sql, timeLimitMs);
    }
}
