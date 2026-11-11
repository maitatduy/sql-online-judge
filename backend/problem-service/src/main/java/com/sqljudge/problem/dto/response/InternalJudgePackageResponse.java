package com.sqljudge.problem.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.sqljudge.problem.entity.StatementType;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record InternalJudgePackageResponse(
        UUID revisionId,
        UUID datasetVersionId,
        String datasetChecksum,
        boolean requiresOrder,
        boolean compareColumnNames,
        double floatTolerance,
        int timeLimitMs,
        Set<StatementType> allowedStatements,
        List<JudgeTestcase> testcases) {

    public InternalJudgePackageResponse {
        allowedStatements = Set.copyOf(allowedStatements);
        testcases = List.copyOf(testcases);
    }

    public record JudgeTestcase(
            UUID testcaseId, int index, UUID variantId, boolean isPublic, ExpectedTable expected) {}

    public record ExpectedTable(List<String> columns, JsonNode rows) {}
}
