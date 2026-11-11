package com.sqljudge.submission.service;

import com.sqljudge.common.outbox.OutboxWriter;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.submission.entity.Submission;
import com.sqljudge.submission.event.SubmissionCreatedEvent;
import com.sqljudge.submission.event.SubmissionTopics;
import com.sqljudge.submission.repository.SubmissionRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class SubmissionWriter {

    private static final int EVENT_VERSION = 1;

    private final SubmissionRepository submissionRepository;
    private final OutboxWriter outboxWriter;
    private final Clock clock;

    @Transactional
    Submission create(Submission pending) {
        Submission submission = submissionRepository.saveAndFlush(pending);
        outboxWriter.append(
                SubmissionTopics.SUBMISSION_CREATED,
                submission.getId().toString(),
                new SubmissionCreatedEvent(
                        UuidV7.generate(),
                        clock.instant(),
                        EVENT_VERSION,
                        submission.getId(),
                        submission.getProblemRevisionId(),
                        submission.getDialect(),
                        submission.getCode(),
                        submission.getAttempts()));
        return submission;
    }
}
