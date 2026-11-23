package com.sqljudge.contest.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sqljudge.contest.event.ContestTopics;
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
                                        record.topic() + ContestTopics.DEAD_LETTER_SUFFIX, -1));
        ExponentialBackOffWithMaxRetries backOff =
                new ExponentialBackOffWithMaxRetries(MAX_RETRIES);
        backOff.setInitialInterval(INITIAL_BACKOFF_MS);
        backOff.setMultiplier(BACKOFF_MULTIPLIER);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(JsonProcessingException.class);
        return errorHandler;
    }

    @Bean
    NewTopic ratingUpdatedTopic() {
        return topic(ContestTopics.RATING_UPDATED);
    }

    @Bean
    NewTopic submissionJudgedDeadLetterTopic() {
        return topic(ContestTopics.SUBMISSION_JUDGED + ContestTopics.DEAD_LETTER_SUFFIX);
    }

    @Bean
    NewTopic contestStartedTopic() {
        return topic(ContestTopics.CONTEST_STARTED);
    }

    @Bean
    NewTopic contestFinishedTopic() {
        return topic(ContestTopics.CONTEST_FINISHED);
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
