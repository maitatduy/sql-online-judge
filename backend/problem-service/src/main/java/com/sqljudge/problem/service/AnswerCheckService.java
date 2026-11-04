package com.sqljudge.problem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.problem.dto.response.AnswerCheckResponse;
import com.sqljudge.problem.dto.response.AnswerCheckResponse.TestcaseResult;
import com.sqljudge.problem.dto.response.DialectCheckResult;
import com.sqljudge.problem.entity.AnswerCheck;
import com.sqljudge.problem.entity.AnswerCheckTestcase;
import com.sqljudge.problem.entity.AnswerCheckTestcase.TestcaseCheck;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.ExpectedResult;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ReferenceSolution;
import com.sqljudge.problem.entity.Testcase;
import com.sqljudge.problem.entity.Verdict;
import com.sqljudge.problem.event.AnswerCheckRequestedEvent;
import com.sqljudge.problem.event.AnswerCheckRequestedEvent.TestcaseJob;
import com.sqljudge.problem.event.AnswerTestcaseCheckedEvent;
import com.sqljudge.problem.event.AnswerTestcaseCheckedEvent.ExpectedTable;
import com.sqljudge.problem.event.OutboxWriter;
import com.sqljudge.problem.event.StudioTopics;
import com.sqljudge.problem.repository.AnswerCheckRepository;
import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerCheckService {

    private static final int EVENT_VERSION = 1;
    private static final TypeReference<List<DialectCheckResult>> DIALECTS =
            new TypeReference<>() {};

    private final ProblemAccess problemAccess;
    private final AnswerCheckRepository answerCheckRepository;
    private final OutboxWriter outboxWriter;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public AnswerCheckResponse start(UUID problemId) {
        Problem problem = problemAccess.editable(problemId);
        ProblemRevision draft = completeDraftOf(problem);
        AnswerCheck check = answerCheckRepository.saveAndFlush(AnswerCheck.pending(problem, draft));
        outboxWriter.append(
                StudioTopics.ANSWER_CHECK_REQUESTED,
                check.getId().toString(),
                new AnswerCheckRequestedEvent(
                        UuidV7.generate(),
                        clock.instant(),
                        EVENT_VERSION,
                        check.getId(),
                        draft.getDatasetVersion().getId(),
                        draft.getDatasetVersion().getChecksum(),
                        solutionsOf(draft),
                        draft.getTestcases().stream()
                                .map(
                                        testcase ->
                                                new TestcaseJob(
                                                        testcase.getId(),
                                                        testcase.getDatasetVariant().getId()))
                                .toList(),
                        problem.isRequiresOrder(),
                        problem.isCompareColumnNames(),
                        problem.getFloatTolerance(),
                        problem.getTimeLimitMs(),
                        problem.getAllowedStatements()));
        return AnswerCheckResponse.of(check, List.of());
    }

    @Transactional(readOnly = true)
    public AnswerCheckResponse get(UUID problemId, UUID checkId) {
        Problem problem = problemAccess.accessible(problemId);
        AnswerCheck check =
                answerCheckRepository
                        .findByIdAndProblemId(checkId, problem.getId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.ANSWER_CHECK_NOT_FOUND,
                                                "Không tìm thấy lần kiểm tra đáp án"));
        return AnswerCheckResponse.of(
                check, check.getTestcases().stream().map(this::resultOf).toList());
    }

    @Transactional
    public void record(AnswerTestcaseCheckedEvent event) {
        Optional<AnswerCheck> pending =
                answerCheckRepository
                        .findById(event.checkId())
                        .filter(check -> !check.hasChecked(event.testcaseId()));
        Optional<Testcase> testcase =
                pending.flatMap(check -> testcaseOf(check.getRevision(), event.testcaseId()));
        if (testcase.isEmpty()) {
            log.info(
                    "answer_testcase_check_skipped checkId={} testcaseId={}",
                    event.checkId(),
                    event.testcaseId());
            return;
        }
        if (event.expected() != null) {
            testcase.get().setExpectedResult(expectedResultOf(event.expected()));
        }
        pending.get().record(testcase.get(), testcaseCheckOf(event), clock.instant());
    }

    private static ProblemRevision completeDraftOf(Problem problem) {
        ProblemRevision draft = problem.getDraftRevision();
        if (draft.getDatasetVersion() == null) {
            throw new BusinessRuleException(
                    ErrorCode.PROBLEM_DATASET_MISSING, "Bài chưa chọn database");
        }
        if (draft.getReferenceSolutions().isEmpty() || draft.getTestcases().isEmpty()) {
            throw new BusinessRuleException(
                    ErrorCode.PROBLEM_ANSWER_INCOMPLETE, "Bài chưa có đáp án chuẩn hoặc testcase");
        }
        return draft;
    }

    private static Map<Dialect, String> solutionsOf(ProblemRevision draft) {
        Map<Dialect, String> solutions = new EnumMap<>(Dialect.class);
        for (ReferenceSolution solution : draft.getReferenceSolutions()) {
            solutions.put(solution.getDialect(), solution.getCode());
        }
        return solutions;
    }

    private static Optional<Testcase> testcaseOf(ProblemRevision revision, UUID testcaseId) {
        return revision.getTestcases().stream()
                .filter(testcase -> testcase.getId().equals(testcaseId))
                .findFirst();
    }

    private ExpectedResult expectedResultOf(ExpectedTable expected) {
        String rowsJson = toJson(expected.rows());
        return new ExpectedResult(
                toJson(expected.columns()),
                rowsJson,
                expected.rows().size(),
                DatasetChecksum.sha256(rowsJson));
    }

    private TestcaseCheck testcaseCheckOf(AnswerTestcaseCheckedEvent event) {
        boolean passed =
                event.systemError() == null
                        && event.expected() != null
                        && !event.dialects().isEmpty()
                        && event.dialects().stream()
                                .allMatch(dialect -> dialect.verdict() == Verdict.AC);
        return new TestcaseCheck(passed, event.systemError(), toJson(event.dialects()));
    }

    private TestcaseResult resultOf(AnswerCheckTestcase testcase) {
        try {
            return new TestcaseResult(
                    testcase.getTestcaseId(),
                    testcase.getPosition(),
                    testcase.isPublic(),
                    testcase.isPassed(),
                    testcase.getSystemError(),
                    objectMapper.readValue(testcase.getDialectsJson(), DIALECTS));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored answer check result is unreadable", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize answer check data", exception);
        }
    }
}
