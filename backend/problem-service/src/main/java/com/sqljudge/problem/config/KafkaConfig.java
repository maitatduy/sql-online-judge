package com.sqljudge.problem.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sqljudge.problem.event.StudioTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaConfig {

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1_000;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic() + StudioTopics.DEAD_LETTER_SUFFIX, -1));
        ExponentialBackOffWithMaxRetries backOff =
                new ExponentialBackOffWithMaxRetries(MAX_RETRIES);
        backOff.setInitialInterval(INITIAL_BACKOFF_MS);
        backOff.setMultiplier(BACKOFF_MULTIPLIER);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(JsonProcessingException.class);
        return errorHandler;
    }

    @Bean
    NewTopic studioRunRequestedTopic() {
        return topic(StudioTopics.RUN_REQUESTED);
    }

    @Bean
    NewTopic studioRunCompletedTopic() {
        return topic(StudioTopics.RUN_COMPLETED);
    }

    @Bean
    NewTopic studioRunCompletedDeadLetterTopic() {
        return topic(StudioTopics.RUN_COMPLETED + StudioTopics.DEAD_LETTER_SUFFIX);
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
