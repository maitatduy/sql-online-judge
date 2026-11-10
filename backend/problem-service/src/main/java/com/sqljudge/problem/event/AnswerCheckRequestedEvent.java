package com.sqljudge.problem.event;

import com.sqljudge.common.outbox.DomainEvent;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.StatementType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record AnswerCheckRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        int version,
        UUID checkId,
        UUID datasetVersionId,
        String datasetChecksum,
        Map<Dialect, String> solutions,
        List<TestcaseJob> testcases,
        boolean requiresOrder,
        boolean compareColumnNames,
        double floatTolerance,
        int timeLimitMs,
        Set<StatementType> allowedStatements)
        implements DomainEvent {

    public record TestcaseJob(UUID testcaseId, UUID variantId) {}
}
