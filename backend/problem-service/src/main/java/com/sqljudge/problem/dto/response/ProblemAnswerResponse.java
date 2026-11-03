package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.DatasetVariant;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ReferenceSolution;
import com.sqljudge.problem.entity.Testcase;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ProblemAnswerResponse(
        List<SolutionResponse> solutions,
        List<TestcaseResponse> testcases,
        List<VariantOptionResponse> variants,
        boolean requiresOrder,
        boolean compareColumnNames,
        double floatTolerance) {

    public record SolutionResponse(Dialect dialect, String sql) {

        static SolutionResponse from(ReferenceSolution solution) {
            return new SolutionResponse(solution.getDialect(), solution.getCode());
        }
    }

    public record TestcaseResponse(UUID variantId, String variantName, boolean isPublic) {

        static TestcaseResponse from(Testcase testcase) {
            return new TestcaseResponse(
                    testcase.getDatasetVariant().getId(),
                    testcase.getDatasetVariant().getName(),
                    testcase.isPublic());
        }
    }

    public record VariantOptionResponse(UUID id, String name, boolean isPublic) {

        static VariantOptionResponse from(DatasetVariant variant) {
            return new VariantOptionResponse(
                    variant.getId(), variant.getName(), variant.isPublic());
        }
    }

    public static ProblemAnswerResponse from(Problem problem) {
        ProblemRevision draft = problem.getDraftRevision();
        List<SolutionResponse> solutions =
                draft.getReferenceSolutions().stream()
                        .sorted(Comparator.comparing(ReferenceSolution::getDialect))
                        .map(SolutionResponse::from)
                        .toList();
        List<TestcaseResponse> testcases =
                draft.getTestcases().stream().map(TestcaseResponse::from).toList();
        List<VariantOptionResponse> variants =
                draft.getDatasetVersion() == null
                        ? List.of()
                        : draft.getDatasetVersion().getVariants().stream()
                                .map(VariantOptionResponse::from)
                                .toList();
        return new ProblemAnswerResponse(
                solutions,
                testcases,
                variants,
                problem.isRequiresOrder(),
                problem.isCompareColumnNames(),
                problem.getFloatTolerance());
    }
}
