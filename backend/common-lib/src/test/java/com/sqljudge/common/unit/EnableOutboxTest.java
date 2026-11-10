package com.sqljudge.common.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.common.outbox.EnableOutbox;
import com.sqljudge.common.outbox.OutboxClaimer;
import com.sqljudge.common.outbox.OutboxEvent;
import com.sqljudge.common.outbox.OutboxEventRepository;
import com.sqljudge.common.outbox.OutboxRelay;
import com.sqljudge.common.outbox.OutboxWriter;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

class EnableOutboxTest {

    private static final String RELAY_PROPERTIES =
            "sqljudge.outbox.relay-interval=500ms,sqljudge.outbox.batch-size=100,"
                    + "sqljudge.outbox.send-timeout=10s,sqljudge.outbox.claim-timeout=60s";

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(OutboxApplication.class)
                    .withPropertyValues(RELAY_PROPERTIES.split(","));

    @Test
    @DisplayName("Bật outbox thì có writer, claimer và gói entity được đăng ký để JPA quét")
    void enableOutbox_registersWriterClaimerAndEntityPackage() {
        runner.run(
                context -> {
                    assertThat(context)
                            .hasSingleBean(OutboxWriter.class)
                            .hasSingleBean(OutboxClaimer.class)
                            .doesNotHaveBean(OutboxRelay.class);
                    assertThat(AutoConfigurationPackages.get(context))
                            .contains(OutboxEvent.class.getPackageName());
                });
    }

    @Test
    @DisplayName("Relay chỉ chạy khi bật sqljudge.outbox.relay-enabled")
    void relayEnabled_registersRelay() {
        runner.withPropertyValues("sqljudge.outbox.relay-enabled=true")
                .run(context -> assertThat(context).hasSingleBean(OutboxRelay.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableOutbox
    static class OutboxApplication {

        @Bean
        OutboxEventRepository outboxEventRepository() {
            return mock(OutboxEventRepository.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }
    }
}
