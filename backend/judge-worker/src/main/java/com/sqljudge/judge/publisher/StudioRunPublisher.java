package com.sqljudge.judge.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.judge.config.JudgeTopics;
import com.sqljudge.judge.job.StudioRunResult;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class StudioRunPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Duration publishTimeout;

    public StudioRunPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${sqljudge.judge.publish-timeout}") Duration publishTimeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.publishTimeout = publishTimeout;
    }

    public void publish(StudioRunResult result) {
        StudioRunCompletedEvent event =
                StudioRunCompletedEvent.of(result, UUID.randomUUID(), clock.instant());
        try {
            kafkaTemplate
                    .send(
                            JudgeTopics.STUDIO_RUN_COMPLETED,
                            result.runId().toString(),
                            objectMapper.writeValueAsString(event))
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException | ExecutionException | TimeoutException exception) {
            throw new IllegalStateException(
                    "Cannot publish studio run result " + result.runId(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while publishing studio run result " + result.runId(), exception);
        }
    }
}
