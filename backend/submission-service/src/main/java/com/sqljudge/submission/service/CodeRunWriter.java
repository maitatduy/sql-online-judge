package com.sqljudge.submission.service;

import com.sqljudge.common.outbox.OutboxWriter;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.submission.entity.CodeRun;
import com.sqljudge.submission.event.RunRequestedEvent;
import com.sqljudge.submission.event.SubmissionTopics;
import com.sqljudge.submission.repository.CodeRunRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CodeRunWriter {

    private static final int EVENT_VERSION = 1;

    private final CodeRunRepository codeRunRepository;
    private final OutboxWriter outboxWriter;
    private final Clock clock;

    @Transactional
    CodeRun create(CodeRun pending) {
        CodeRun run = codeRunRepository.saveAndFlush(pending);
        outboxWriter.append(
                SubmissionTopics.RUN_REQUESTED,
                run.getId().toString(),
                new RunRequestedEvent(
                        UuidV7.generate(),
                        clock.instant(),
                        EVENT_VERSION,
                        run.getId(),
                        run.getProblemRevisionId(),
                        run.getDialect(),
                        run.getCode()));
        return run;
    }
}
