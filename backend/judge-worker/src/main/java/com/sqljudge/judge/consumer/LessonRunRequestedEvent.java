package com.sqljudge.judge.consumer;

import com.sqljudge.judge.job.LessonRunRequest;
import com.sqljudge.judge.sandbox.Dialect;
import java.time.Instant;
import java.util.UUID;

public record LessonRunRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID runId,
        UUID datasetVersionId,
        UUID datasetVariantId,
        String datasetChecksum,
        Dialect dialect,
        String sql,
        int timeLimitMs) {

    LessonRunRequest toRequest() {
        return new LessonRunRequest(
                runId,
                datasetVersionId,
                datasetVariantId,
                datasetChecksum,
                dialect,
                sql,
                timeLimitMs);
    }
}
