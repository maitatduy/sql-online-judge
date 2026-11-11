package com.sqljudge.judge.job;

import com.sqljudge.judge.compare.CompareOptions;
import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.guard.StatementType;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record JudgePackage(
        UUID datasetVersionId,
        String datasetChecksum,
        CompareOptions compareOptions,
        int timeLimitMs,
        Set<StatementType> allowedStatements,
        List<JudgeCase> testcases) {

    public JudgePackage {
        allowedStatements = Set.copyOf(allowedStatements);
        testcases = List.copyOf(testcases);
    }

    public record JudgeCase(
            int index, UUID variantId, boolean isPublic, NormalizedTable expected) {}
}
