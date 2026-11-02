package com.sqljudge.problem.event;

import com.sqljudge.problem.dto.response.StudioRunOutcome;
import com.sqljudge.problem.dto.response.StudioRunOutcome.Rejection;
import com.sqljudge.problem.dto.response.StudioRunOutcome.VariantResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudioRunCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID runId,
        Rejection rejection,
        String systemError,
        List<VariantResult> variants) {

    public StudioRunOutcome outcome() {
        return new StudioRunOutcome(rejection, systemError, variants);
    }
}
