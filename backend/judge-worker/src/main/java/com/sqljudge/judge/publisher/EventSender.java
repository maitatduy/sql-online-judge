package com.sqljudge.judge.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class EventSender {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Duration publishTimeout;

    EventSender(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${sqljudge.judge.publish-timeout}") Duration publishTimeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.publishTimeout = publishTimeout;
    }

    void send(String topic, String key, Object event) {
        try {
            kafkaTemplate
                    .send(topic, key, objectMapper.writeValueAsString(event))
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException | ExecutionException | TimeoutException exception) {
            throw new IllegalStateException(
                    "Cannot publish to " + topic + " with key " + key, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while publishing to " + topic + " with key " + key, exception);
        }
    }
}
