package com.sqljudge.judge.publisher;

import com.sqljudge.judge.job.StudioRunResult;
import com.sqljudge.judge.job.StudioRunResult.Rejection;
import com.sqljudge.judge.job.VariantRunResult;
import com.sqljudge.judge.sandbox.Dialect;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudioRunCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID runId,
        Dialect dialect,
        Rejection rejection,
        String systemError,
        List<VariantRunResult> variants) {

    static final int VERSION = 1;

    static StudioRunCompletedEvent of(StudioRunResult result, UUID eventId, Instant occurredAt) {
        return new StudioRunCompletedEvent(
                eventId,
                occurredAt,
                VERSION,
                result.runId(),
                result.dialect(),
                result.rejection(),
                result.systemError(),
                result.variants());
    }
}
