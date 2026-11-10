package com.sqljudge.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
@AutoConfigurationPackage(basePackageClasses = OutboxEvent.class)
class OutboxConfiguration {

    @Bean
    OutboxWriter outboxWriter(OutboxEventRepository repository, ObjectMapper objectMapper) {
        return new OutboxWriter(repository, objectMapper);
    }

    @Bean
    OutboxClaimer outboxClaimer(
            OutboxEventRepository repository, OutboxProperties properties, Clock clock) {
        return new OutboxClaimer(repository, properties, clock);
    }

    @Bean
    @ConditionalOnProperty(prefix = "sqljudge.outbox", name = "relay-enabled", havingValue = "true")
    OutboxRelay outboxRelay(
            OutboxClaimer claimer,
            KafkaTemplate<String, String> kafkaTemplate,
            OutboxProperties properties) {
        return new OutboxRelay(claimer, kafkaTemplate, properties);
    }
}
