package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.problem.config.CacheConfig;
import com.sqljudge.problem.dto.response.InternalContestProblemResponse;
import com.sqljudge.problem.dto.response.InternalContestStatementResponse;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.repository.ProblemRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalContestProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemStatementParts statementParts;

    @Transactional(readOnly = true)
    public List<InternalContestProblemResponse> contestReady(List<UUID> problemIds) {
        return problemRepository
                .findByIdInAndStatusAndVisibilityAndDeletedAtIsNull(
                        problemIds, ProblemStatus.PUBLISHED, ProblemVisibility.CONTEST_ONLY)
                .stream()
                .map(InternalContestProblemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.CONTEST_STATEMENT, key = "#problemId")
    public InternalContestStatementResponse statement(UUID problemId) {
        Problem problem =
                problemRepository
                        .findByIdAndDeletedAtIsNull(problemId)
                        .filter(candidate -> candidate.getStatus() == ProblemStatus.PUBLISHED)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.PROBLEM_NOT_FOUND,
                                                "Không tìm thấy bài tập"));
        ProblemRevision revision = problem.getPublishedRevision();
        List<Dialect> dialects = statementParts.dialectsOf(revision);
        return new InternalContestStatementResponse(
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getStatementMarkdown(),
                dialects,
                statementParts.engineVersionsOf(dialects),
                problem.getStarterCode(),
                statementParts.schemaOf(revision),
                statementParts.examplesOf(revision),
                problem.isRequiresOrder(),
                problem.getAllowedStatements().stream().sorted().toList(),
                problem.getTimeLimitMs(),
                statementParts.hiddenTestcaseCountOf(revision));
    }
}
