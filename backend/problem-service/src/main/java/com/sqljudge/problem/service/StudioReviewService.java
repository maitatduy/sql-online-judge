package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.problem.dto.response.StudioProblemResponse;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.repository.ProblemRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudioReviewService {

    private final ProblemAccess problemAccess;
    private final ProblemRepository problemRepository;
    private final AnswerCheckService answerCheckService;

    @Transactional
    public StudioProblemResponse submitForReview(UUID problemId) {
        Problem problem = problemAccess.editable(problemId);
        if (problem.getStatementMarkdown().isBlank()) {
            throw new BusinessRuleException(
                    ErrorCode.PROBLEM_STATEMENT_MISSING, "Bài chưa có đề bài");
        }
        if (!answerCheckService.isAnswerVerified(problem)) {
            throw new BusinessRuleException(
                    ErrorCode.PROBLEM_ANSWER_NOT_VERIFIED,
                    "Đáp án chưa đạt kiểm tra hoặc đã sửa sau lần kiểm tra gần nhất");
        }
        problem.setStatus(ProblemStatus.PENDING_REVIEW);
        Problem submitted = problemRepository.saveAndFlush(problem);
        log.info("problem_submitted_for_review problemId={}", submitted.getId());
        return StudioProblemResponse.from(submitted);
    }
}
