package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.problem.dto.response.InternalJudgingInfoResponse;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.entity.ReferenceSolution;
import com.sqljudge.problem.repository.ProblemRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalJudgingInfoService {

    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public InternalJudgingInfoResponse judgingInfo(UUID problemId) {
        Problem problem =
                problemRepository
                        .findByIdAndStatusAndVisibilityAndDeletedAtIsNull(
                                problemId, ProblemStatus.PUBLISHED, ProblemVisibility.PUBLIC)
                        .orElseThrow(InternalJudgingInfoService::problemNotFound);
        return toResponse(problem, problem.getNumber(), problem.getSlug());
    }

    @Transactional(readOnly = true)
    public InternalJudgingInfoResponse contestJudgingInfo(UUID problemId) {
        Problem problem =
                problemRepository
                        .findByIdAndDeletedAtIsNull(problemId)
                        .filter(candidate -> candidate.getStatus() == ProblemStatus.PUBLISHED)
                        .orElseThrow(InternalJudgingInfoService::problemNotFound);
        return toResponse(problem, null, null);
    }

    private static InternalJudgingInfoResponse toResponse(
            Problem problem, Integer number, String slug) {
        ProblemRevision revision = problem.getPublishedRevision();
        return new InternalJudgingInfoResponse(
                problem.getId(),
                revision.getId(),
                revision.getReferenceSolutions().stream()
                        .map(ReferenceSolution::getDialect)
                        .sorted()
                        .toList(),
                revision.getTestcases().size(),
                problem.getTimeLimitMs(),
                number,
                slug,
                problem.getTitle());
    }

    private static NotFoundException problemNotFound() {
        return new NotFoundException(ErrorCode.PROBLEM_NOT_FOUND, "Không tìm thấy bài tập");
    }
}
