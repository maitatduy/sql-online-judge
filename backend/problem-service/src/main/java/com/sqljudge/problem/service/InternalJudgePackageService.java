package com.sqljudge.problem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.problem.dto.response.InternalJudgePackageResponse;
import com.sqljudge.problem.dto.response.InternalJudgePackageResponse.ExpectedTable;
import com.sqljudge.problem.dto.response.InternalJudgePackageResponse.JudgeTestcase;
import com.sqljudge.problem.entity.ExpectedResult;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.Testcase;
import com.sqljudge.problem.repository.ProblemRevisionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalJudgePackageService {

    private static final TypeReference<List<String>> COLUMNS = new TypeReference<>() {};

    private final ProblemRevisionRepository problemRevisionRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public InternalJudgePackageResponse judgePackage(UUID revisionId) {
        ProblemRevision revision =
                problemRevisionRepository
                        .findByIdAndFrozenAtIsNotNull(revisionId)
                        .filter(ProblemRevision::hasExpectedResults)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.PROBLEM_REVISION_NOT_FOUND,
                                                "Không tìm thấy bản đã chốt của bài"));
        Problem problem = revision.getProblem();
        return new InternalJudgePackageResponse(
                revision.getId(),
                revision.getDatasetVersion().getId(),
                revision.getDatasetVersion().getChecksum(),
                problem.isRequiresOrder(),
                problem.isCompareColumnNames(),
                problem.getFloatTolerance(),
                problem.getTimeLimitMs(),
                problem.getAllowedStatements(),
                revision.getTestcases().stream().map(this::testcaseOf).toList());
    }

    private JudgeTestcase testcaseOf(Testcase testcase) {
        return new JudgeTestcase(
                testcase.getId(),
                testcase.getPosition() + 1,
                testcase.getDatasetVariant().getId(),
                testcase.isPublic(),
                expectedOf(testcase.getExpectedResult()));
    }

    private ExpectedTable expectedOf(ExpectedResult result) {
        try {
            return new ExpectedTable(
                    objectMapper.readValue(result.getColumnsJson(), COLUMNS),
                    objectMapper.readTree(result.getRowsJson()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored expected result is not valid JSON", exception);
        }
    }
}
