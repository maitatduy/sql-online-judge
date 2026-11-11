package com.sqljudge.submission.config;

import com.sqljudge.submission.event.SubmissionTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;

    @Bean
    NewTopic submissionCreatedTopic() {
        return TopicBuilder.name(SubmissionTopics.SUBMISSION_CREATED)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
}
