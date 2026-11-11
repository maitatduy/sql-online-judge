package com.sqljudge.judge.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sqljudge.judge.guard.StatementType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record JudgePackageResponse(
        UUID revisionId,
        UUID datasetVersionId,
        String datasetChecksum,
        boolean requiresOrder,
        boolean compareColumnNames,
        BigDecimal floatTolerance,
        int timeLimitMs,
        Set<StatementType> allowedStatements,
        List<PackageTestcase> testcases) {

    public record PackageTestcase(
            UUID testcaseId,
            int index,
            UUID variantId,
            boolean isPublic,
            ExpectedResult expected) {}

    public record ExpectedResult(List<String> columns, JsonNode rows) {}
}
