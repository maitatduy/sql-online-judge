package com.sqljudge.judge.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.judge.config.JudgeTopics;
import com.sqljudge.judge.job.StudioRunProcessor;
import com.sqljudge.judge.job.StudioRunResult;
import com.sqljudge.judge.publisher.StudioRunPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class StudioRunConsumer {

    private static final Logger log = LoggerFactory.getLogger(StudioRunConsumer.class);

    private final ObjectMapper objectMapper;
    private final StudioRunProcessor processor;
    private final StudioRunPublisher publisher;

    public StudioRunConsumer(
            ObjectMapper objectMapper, StudioRunProcessor processor, StudioRunPublisher publisher) {
        this.objectMapper = objectMapper;
        this.processor = processor;
        this.publisher = publisher;
    }

    @KafkaListener(
            topics = JudgeTopics.STUDIO_RUN_REQUESTED,
            concurrency = "${sqljudge.judge.consumer-concurrency}")
    public void onStudioRunRequested(String payload) throws JsonProcessingException {
        StudioRunRequestedEvent event =
                objectMapper.readValue(payload, StudioRunRequestedEvent.class);
        StudioRunResult result = processor.process(event.toRequest());
        publisher.publish(result);
        log.info(
                "studio_run_processed runId={} dialect={} variants={} systemError={}",
                result.runId(),
                result.dialect(),
                result.variants().size(),
                result.systemError());
    }
}
