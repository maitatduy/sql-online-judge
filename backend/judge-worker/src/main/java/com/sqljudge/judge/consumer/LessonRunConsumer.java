package com.sqljudge.judge.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.judge.config.JudgeTopics;
import com.sqljudge.judge.job.CodeRunResult;
import com.sqljudge.judge.job.LessonRunProcessor;
import com.sqljudge.judge.job.RunOutcome;
import com.sqljudge.judge.publisher.LessonRunPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LessonRunConsumer {

    private static final Logger log = LoggerFactory.getLogger(LessonRunConsumer.class);

    private final ObjectMapper objectMapper;
    private final LessonRunProcessor processor;
    private final LessonRunPublisher publisher;

    public LessonRunConsumer(
            ObjectMapper objectMapper, LessonRunProcessor processor, LessonRunPublisher publisher) {
        this.objectMapper = objectMapper;
        this.processor = processor;
        this.publisher = publisher;
    }

    @KafkaListener(
            topics = JudgeTopics.LESSON_RUN_REQUESTED,
            concurrency = "${sqljudge.judge.consumer-concurrency}")
    public void onLessonRunRequested(String payload) throws JsonProcessingException {
        LessonRunRequestedEvent event =
                objectMapper.readValue(payload, LessonRunRequestedEvent.class);
        CodeRunResult result = run(event);
        publisher.completed(result);
        log.info(
                "lesson_run_completed runId={} outcome={} sqlLength={}",
                event.runId(),
                result.outcome(),
                event.sql().length());
    }

    private CodeRunResult run(LessonRunRequestedEvent event) {
        try {
            return processor.run(event.toRequest());
        } catch (RuntimeException exception) {
            log.error("lesson_run_failed runId={}", event.runId(), exception);
            return CodeRunResult.failed(event.runId(), RunOutcome.SE, 0, null);
        }
    }
}
