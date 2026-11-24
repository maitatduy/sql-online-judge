package com.sqljudge.submission.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.outbox.OutboxWriter;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.submission.client.JudgingInfo;
import com.sqljudge.submission.dto.response.RejudgeBatchResponse;
import com.sqljudge.submission.entity.RejudgeBatch;
import com.sqljudge.submission.entity.RejudgeStatus;
import com.sqljudge.submission.entity.Submission;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.event.RejudgeFinishedEvent;
import com.sqljudge.submission.event.SubmissionCreatedEvent;
import com.sqljudge.submission.event.SubmissionTopics;
import com.sqljudge.submission.repository.RejudgeBatchRepository;
import com.sqljudge.submission.repository.SubmissionRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RejudgeWriter {

    private static final int EVENT_VERSION = 1;

    private final RejudgeBatchRepository batchRepository;
    private final SubmissionRepository submissionRepository;
    private final OutboxWriter outboxWriter;
    private final Clock clock;

    @Transactional
    public RejudgeBatchResponse start(JudgingInfo problem) {
        requireNoRunningBatch(problem.problemId());
        List<Submission> submissions =
                submissionRepository.findPracticeToRejudge(
                        problem.problemId(), SubmissionStatus.COMPLETED);
        RejudgeBatch batch =
                batchRepository.save(
                        RejudgeBatch.start(
                                problem.problemId(), problem.revisionId(), submissions.size()));
        submissions.forEach(
                submission -> {
                    submission.rejudge(
                            problem.revisionId(), problem.testcaseCount(), batch.getId());
                    queueRejudge(submission);
                });
        finishIfDone(batch);
        log.info(
                "rejudge_started problemId={} batchId={} submissions={}",
                problem.problemId(),
                batch.getId(),
                submissions.size());
        return RejudgeBatchResponse.from(batch);
    }

    @Transactional(readOnly = true)
    public void requireNoRunningBatch(UUID problemId) {
        if (batchRepository.existsByProblemIdAndStatus(problemId, RejudgeStatus.RUNNING)) {
            throw new ConflictException(
                    ErrorCode.REJUDGE_IN_PROGRESS, "Bài này đang được chấm lại");
        }
    }

    void countDown(UUID batchId) {
        batchRepository.countDown(batchId);
        batchRepository.findById(batchId).ifPresent(this::finishIfDone);
    }

    private void finishIfDone(RejudgeBatch batch) {
        if (!batch.finishIfDone(clock.instant())) {
            return;
        }
        outboxWriter.append(
                SubmissionTopics.REJUDGE_FINISHED,
                batch.getProblemId().toString(),
                new RejudgeFinishedEvent(
                        UuidV7.generate(),
                        clock.instant(),
                        EVENT_VERSION,
                        batch.getProblemId(),
                        batch.getId()));
        log.info("rejudge_finished problemId={} batchId={}", batch.getProblemId(), batch.getId());
    }

    private void queueRejudge(Submission submission) {
        outboxWriter.append(
                SubmissionTopics.REJUDGE_REQUESTED,
                submission.getId().toString(),
                new SubmissionCreatedEvent(
                        UuidV7.generate(),
                        clock.instant(),
                        EVENT_VERSION,
                        submission.getId(),
                        submission.getProblemRevisionId(),
                        submission.getDialect(),
                        submission.getCode(),
                        submission.getAttempts(),
                        !submission.isInContest()));
    }
}
