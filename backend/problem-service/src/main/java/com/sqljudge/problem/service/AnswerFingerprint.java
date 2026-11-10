package com.sqljudge.problem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ReferenceSolution;
import com.sqljudge.problem.entity.StatementType;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

final class AnswerFingerprint {

    private static final ObjectMapper JSON = new ObjectMapper();

    private record CheckedAnswer(
            UUID datasetVersionId,
            Map<Dialect, String> solutions,
            List<UUID> testcaseVariantIds,
            boolean requiresOrder,
            boolean compareColumnNames,
            double floatTolerance,
            int timeLimitMs,
            TreeSet<StatementType> allowedStatements) {}

    private AnswerFingerprint() {}

    static String of(Problem problem, ProblemRevision revision) {
        Map<Dialect, String> solutions = new TreeMap<>();
        for (ReferenceSolution solution : revision.getReferenceSolutions()) {
            solutions.put(solution.getDialect(), solution.getCode());
        }
        CheckedAnswer answer =
                new CheckedAnswer(
                        revision.getDatasetVersion().getId(),
                        solutions,
                        revision.getTestcases().stream()
                                .map(testcase -> testcase.getDatasetVariant().getId())
                                .toList(),
                        problem.isRequiresOrder(),
                        problem.isCompareColumnNames(),
                        problem.getFloatTolerance(),
                        problem.getTimeLimitMs(),
                        new TreeSet<>(problem.getAllowedStatements()));
        try {
            return DatasetChecksum.sha256(JSON.writeValueAsString(answer));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot fingerprint the problem answer", exception);
        }
    }
}
