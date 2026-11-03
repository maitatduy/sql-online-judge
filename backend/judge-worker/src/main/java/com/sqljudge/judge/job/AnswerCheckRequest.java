package com.sqljudge.judge.job;

import com.sqljudge.judge.compare.CompareOptions;
import com.sqljudge.judge.guard.StatementType;
import com.sqljudge.judge.sandbox.Dialect;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record AnswerCheckRequest(
        UUID checkId,
        UUID datasetVersionId,
        String datasetChecksum,
        Map<Dialect, String> solutions,
        List<AnswerCheckTestcase> testcases,
        CompareOptions compareOptions,
        int timeLimitMs,
        Set<StatementType> allowedStatements) {

    public AnswerCheckRequest {
        if (solutions.isEmpty()) {
            throw new IllegalArgumentException("Answer check needs at least one solution");
        }
        solutions = new EnumMap<>(solutions);
        testcases = List.copyOf(testcases);
        allowedStatements = Set.copyOf(allowedStatements);
    }
}
