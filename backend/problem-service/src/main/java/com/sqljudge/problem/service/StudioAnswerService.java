package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.problem.dto.request.SaveProblemAnswerRequest;
import com.sqljudge.problem.dto.request.SaveProblemAnswerRequest.SolutionRequest;
import com.sqljudge.problem.dto.response.ProblemAnswerResponse;
import com.sqljudge.problem.entity.DatasetVariant;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.repository.ProblemRepository;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudioAnswerService {

    private static final String SOLUTIONS_FIELD = "solutions";
    private static final String TESTCASES_FIELD = "testcaseVariantIds";

    private final ProblemAccess problemAccess;
    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public ProblemAnswerResponse get(UUID problemId) {
        return ProblemAnswerResponse.from(problemAccess.accessible(problemId));
    }

    @Transactional
    public ProblemAnswerResponse save(UUID problemId, SaveProblemAnswerRequest request) {
        Problem problem = problemAccess.editable(problemId);
        ProblemRevision draft = problem.getDraftRevision();
        DatasetVersion datasetVersion = draft.getDatasetVersion();
        if (datasetVersion == null) {
            throw new BusinessRuleException(
                    ErrorCode.PROBLEM_DATASET_MISSING, "Bài chưa chọn database");
        }
        draft.replaceReferenceSolutions(codeByDialect(request.solutions()));
        draft.replaceTestcases(variantsOf(datasetVersion, request.testcaseVariantIds()));
        problem.setRequiresOrder(request.requiresOrder());
        problem.setCompareColumnNames(request.compareColumnNames());
        problem.setFloatTolerance(request.floatTolerance());
        return ProblemAnswerResponse.from(problemRepository.saveAndFlush(problem));
    }

    private static Map<Dialect, String> codeByDialect(List<SolutionRequest> solutions) {
        Map<Dialect, String> codeByDialect = new LinkedHashMap<>();
        for (SolutionRequest solution : solutions) {
            if (codeByDialect.put(solution.dialect(), solution.sql()) != null) {
                throw new InvalidFieldException(SOLUTIONS_FIELD, "Mỗi dialect chỉ có một đáp án");
            }
        }
        if (!codeByDialect.containsKey(Dialect.POSTGRESQL)) {
            throw new InvalidFieldException(SOLUTIONS_FIELD, "Cần có đáp án PostgreSQL");
        }
        return codeByDialect;
    }

    private static List<DatasetVariant> variantsOf(DatasetVersion version, List<UUID> variantIds) {
        if (new HashSet<>(variantIds).size() != variantIds.size()) {
            throw new InvalidFieldException(TESTCASES_FIELD, "Mỗi bộ dữ liệu chỉ chọn một lần");
        }
        return variantIds.stream()
                .map(
                        variantId ->
                                version.findVariant(variantId)
                                        .orElseThrow(
                                                () ->
                                                        new InvalidFieldException(
                                                                TESTCASES_FIELD,
                                                                "Bộ dữ liệu không thuộc database"
                                                                        + " của bài")))
                .toList();
    }
}
