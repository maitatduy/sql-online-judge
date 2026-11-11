package com.sqljudge.submission.service;

import com.sqljudge.common.outbox.OutboxWriter;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.submission.entity.JudgeOutcome;
import com.sqljudge.submission.entity.JudgeResult;
import com.sqljudge.submission.entity.Submission;
import com.sqljudge.submission.entity.Verdict;
import com.sqljudge.submission.event.SubmissionChanged;
import com.sqljudge.submission.event.SubmissionJudgedEvent;
import com.sqljudge.submission.event.SubmissionTopics;
import com.sqljudge.submission.repository.SubmissionRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeResultService {

    private static final int EVENT_VERSION = 1;

    private final SubmissionRepository submissionRepository;
    private final SubmissionWriter submissionWriter;
    private final OutboxWriter outboxWriter;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void recordProgress(UUID submissionId, int attempt, int judgedTestcases) {
        submissionRepository
                .findById(submissionId)
                .ifPresentOrElse(
                        submission -> {
                            if (submission.recordProgress(attempt, judgedTestcases)) {
                                eventPublisher.publishEvent(new SubmissionChanged(submissionId));
                            }
                        },
                        () -> logUnknown(submissionId));
    }

    @Transactional
    public void complete(UUID submissionId, JudgeResult result) {
        submissionRepository
                .findById(submissionId)
                .ifPresentOrElse(
                        submission -> apply(submission, result), () -> logUnknown(submissionId));
    }

    private void apply(Submission submission, JudgeResult result) {
        JudgeOutcome outcome = submission.complete(result, clock.instant());
        if (outcome != JudgeOutcome.IGNORED) {
            eventPublisher.publishEvent(new SubmissionChanged(submission.getId()));
        }
        if (outcome == JudgeOutcome.RETRY) {
            log.warn(
                    "submission_rejudge_after_system_error submissionId={} attempt={}",
                    submission.getId(),
                    submission.getAttempts());
            submissionWriter.queueJudging(submission);
        }
        if (outcome == JudgeOutcome.COMPLETED) {
            logCompleted(submission);
            queueJudgedEvent(submission);
        }
    }

    private void queueJudgedEvent(Submission submission) {
        outboxWriter.append(
                SubmissionTopics.SUBMISSION_JUDGED,
                submission.getId().toString(),
                new SubmissionJudgedEvent(
                        UuidV7.generate(),
                        clock.instant(),
                        EVENT_VERSION,
                        submission.getId(),
                        submission.getUserId(),
                        submission.getProblemId(),
                        submission.getVerdict(),
                        submission.getJudgedAt()));
    }

    private static void logCompleted(Submission submission) {
        if (submission.getVerdict() == Verdict.SE) {
            log.warn("submission_system_error_final submissionId={}", submission.getId());
            return;
        }
        log.info(
                "submission_judged submissionId={} verdict={}",
                submission.getId(),
                submission.getVerdict());
    }

    private static void logUnknown(UUID submissionId) {
        log.warn("judge_result_unknown_submission submissionId={}", submissionId);
    }
}
