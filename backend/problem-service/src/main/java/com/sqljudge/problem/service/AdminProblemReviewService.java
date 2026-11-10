package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.outbox.OutboxWriter;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.problem.dto.request.ReviewProblemRequest;
import com.sqljudge.problem.dto.response.StudioProblemResponse;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.event.ProblemPublishedEvent;
import com.sqljudge.problem.event.ProblemTopics;
import com.sqljudge.problem.repository.ProblemRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProblemReviewService {

    private static final int EVENT_VERSION = 1;

    private final ProblemRepository problemRepository;
    private final AnswerCheckService answerCheckService;
    private final ProblemDetailCache problemDetailCache;
    private final OutboxWriter outboxWriter;
    private final Clock clock;

    @Transactional
    public StudioProblemResponse approve(UUID problemId) {
        Problem problem = pendingProblem(problemId);
        if (!answerCheckService.isAnswerVerified(problem)
                || !problem.getDraftRevision().hasExpectedResults()) {
            throw new BusinessRuleException(
                    ErrorCode.PROBLEM_ANSWER_NOT_VERIFIED,
                    "Đáp án chưa đạt kiểm tra hoặc đã sửa sau lần kiểm tra gần nhất");
        }
        Instant now = clock.instant();
        problem.publish(
                reviewerId(),
                problemRepository.findMaxNumber() + 1,
                AnswerFingerprint.of(problem, problem.getDraftRevision()),
                now);
        Problem published = problemRepository.saveAndFlush(problem);
        outboxWriter.append(
                ProblemTopics.PROBLEM_PUBLISHED,
                published.getId().toString(),
                new ProblemPublishedEvent(
                        UuidV7.generate(),
                        now,
                        EVENT_VERSION,
                        published.getId(),
                        published.getNumber(),
                        published.getSlug(),
                        published.getTitle(),
                        published.getDifficulty(),
                        published.getAuthorId()));
        evictDetailAfterCommit(published.getSlug());
        log.info(
                "problem_published problemId={} number={}",
                published.getId(),
                published.getNumber());
        return StudioProblemResponse.from(published);
    }

    @Transactional
    public StudioProblemResponse requestChanges(UUID problemId, ReviewProblemRequest request) {
        Problem problem = pendingProblem(problemId);
        problem.requestChanges(reviewerId(), request.reason().trim());
        log.info("problem_changes_requested problemId={}", problem.getId());
        return StudioProblemResponse.from(problemRepository.saveAndFlush(problem));
    }

    @Transactional
    public StudioProblemResponse reject(UUID problemId, ReviewProblemRequest request) {
        Problem problem = pendingProblem(problemId);
        problem.reject(reviewerId(), request.reason().trim());
        log.info("problem_rejected problemId={}", problem.getId());
        return StudioProblemResponse.from(problemRepository.saveAndFlush(problem));
    }

    private Problem pendingProblem(UUID problemId) {
        Problem problem =
                problemRepository
                        .findByIdAndDeletedAtIsNull(problemId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.PROBLEM_NOT_FOUND,
                                                "Không tìm thấy bài tập"));
        if (!problem.isPendingReview()) {
            throw new BusinessRuleException(
                    ErrorCode.PROBLEM_NOT_PENDING_REVIEW, "Bài không ở trạng thái chờ duyệt");
        }
        return problem;
    }

    private static UUID reviewerId() {
        return SecurityUtils.requireCurrentUser().id();
    }

    private void evictDetailAfterCommit(String slug) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        problemDetailCache.evict(slug);
                    }
                });
    }
}
