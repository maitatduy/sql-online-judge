package com.sqljudge.learning.service;

import com.sqljudge.common.outbox.OutboxWriter;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.learning.entity.LessonCell;
import com.sqljudge.learning.entity.LessonRun;
import com.sqljudge.learning.event.LearningTopics;
import com.sqljudge.learning.event.LessonRunRequestedEvent;
import com.sqljudge.learning.repository.LessonRunRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class LessonRunWriter {

    private static final int EVENT_VERSION = 1;
    private static final int TIME_LIMIT_MS = 2000;

    private final LessonRunRepository lessonRunRepository;
    private final OutboxWriter outboxWriter;
    private final Clock clock;

    @Transactional
    LessonRun create(LessonRun pending, LessonCell cell) {
        LessonRun run = lessonRunRepository.saveAndFlush(pending);
        outboxWriter.append(
                LearningTopics.LESSON_RUN_REQUESTED,
                run.getId().toString(),
                new LessonRunRequestedEvent(
                        UuidV7.generate(),
                        clock.instant(),
                        EVENT_VERSION,
                        run.getId(),
                        cell.getDatasetVersionId(),
                        cell.getDatasetVariantId(),
                        cell.getDatasetChecksum(),
                        run.getDialect(),
                        run.getSql(),
                        TIME_LIMIT_MS));
        return run;
    }
}
