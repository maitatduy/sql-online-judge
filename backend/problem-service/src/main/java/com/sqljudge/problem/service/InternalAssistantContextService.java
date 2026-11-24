package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.problem.dto.response.InternalAssistantContextResponse;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemProgressStatus;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.UserProblemProgressRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalAssistantContextService {

    private final ProblemRepository problemRepository;
    private final UserProblemProgressRepository progressRepository;
    private final ProblemStatementParts statementParts;

    @Transactional(readOnly = true)
    public InternalAssistantContextResponse context(UUID problemId, UUID userId) {
        Problem problem =
                problemRepository
                        .findByIdAndStatusAndVisibilityAndDeletedAtIsNull(
                                problemId, ProblemStatus.PUBLISHED, ProblemVisibility.PUBLIC)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.PROBLEM_NOT_FOUND,
                                                "Không tìm thấy bài tập"));
        boolean isSolved =
                progressRepository
                        .findByUserIdAndProblemId(userId, problemId)
                        .filter(progress -> progress.getStatus() == ProblemProgressStatus.SOLVED)
                        .isPresent();
        return new InternalAssistantContextResponse(
                problem.getId(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getStatementMarkdown(),
                statementParts.dialectsOf(problem.getPublishedRevision()),
                statementParts.schemaOf(problem.getPublishedRevision()),
                isSolved);
    }
}
