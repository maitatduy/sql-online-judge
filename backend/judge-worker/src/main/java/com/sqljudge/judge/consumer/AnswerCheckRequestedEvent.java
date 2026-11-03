package com.sqljudge.judge.consumer;

import com.sqljudge.judge.compare.CompareOptions;
import com.sqljudge.judge.guard.StatementType;
import com.sqljudge.judge.job.AnswerCheckRequest;
import com.sqljudge.judge.job.AnswerCheckTestcase;
import com.sqljudge.judge.sandbox.Dialect;
import java.math.BigDecimal;
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
        List<AnswerCheckTestcase> testcases,
        boolean requiresOrder,
        boolean compareColumnNames,
        BigDecimal floatTolerance,
        int timeLimitMs,
        Set<StatementType> allowedStatements) {

    AnswerCheckRequest toRequest() {
        BigDecimal tolerance =
                floatTolerance == null ? CompareOptions.DEFAULT_TOLERANCE : floatTolerance;
        return new AnswerCheckRequest(
                checkId,
                datasetVersionId,
                datasetChecksum,
                solutions,
                testcases,
                new CompareOptions(requiresOrder, compareColumnNames, tolerance),
                timeLimitMs,
                allowedStatements == null ? Set.of() : allowedStatements);
    }
}
