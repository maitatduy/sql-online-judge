package com.sqljudge.contest.config;

import com.sqljudge.contest.event.ContestTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;

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
