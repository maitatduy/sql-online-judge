package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.common.outbox.DomainEvent;
import com.sqljudge.common.outbox.OutboxWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(properties = "sqljudge.outbox.relay-enabled=true")
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class OutboxIntegrationTest extends ContainerSupport {

    private static final String TOPIC = "sqljudge.test.outbox-checked";
    private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(20);

    @ServiceConnection
    static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka:3.9.1")
                    .withEnv("KAFKA_HEAP_OPTS", "-Xms256m -Xmx256m");

    static {
        KAFKA.start();
    }

    @Autowired private OutboxWriter outboxWriter;

    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    record OutboxChecked(UUID eventId, Instant occurredAt, int version, String note)
            implements DomainEvent {}

    @AfterEach
    void deleteOutbox() {
        jdbcTemplate.update("DELETE FROM outbox_events");
    }

    @Test
    @DisplayName("Event ghi trong transaction được đẩy lên Kafka kèm key, rồi đánh dấu đã gửi")
    void append_committed_isPublishedToKafka() {
        UUID eventId = UUID.randomUUID();
        OutboxChecked event =
                new OutboxChecked(eventId, Instant.parse("2026-11-02T08:00:00Z"), 1, "xin chào");

        transactionTemplate.executeWithoutResult(
                status -> outboxWriter.append(TOPIC, "problem-1", event));

        ConsumerRecord<String, String> record = firstRecordOf(TOPIC);
        assertThat(record.key()).isEqualTo("problem-1");
        DocumentContext payload = JsonPath.parse(record.value());
        assertThat(payload.read("$.eventId", String.class)).isEqualTo(eventId.toString());
        assertThat(payload.read("$.occurredAt", String.class)).isEqualTo("2026-11-02T08:00:00Z");
        assertThat(payload.read("$.version", Integer.class)).isEqualTo(1);
        assertThat(payload.read("$.note", String.class)).isEqualTo("xin chào");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM outbox_events WHERE published_at IS NULL",
                                Long.class))
                .isZero();
    }

    @Test
    @DisplayName("Transaction bị rollback thì không còn event nào trong outbox")
    void append_rolledBack_leavesNothing() {
        assertThatThrownBy(
                        () ->
                                transactionTemplate.executeWithoutResult(
                                        status -> {
                                            outboxWriter.append(TOPIC, "problem-2", newEvent());
                                            throw new IllegalStateException("rollback");
                                        }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events", Long.class))
                .isZero();
    }

    @Test
    @DisplayName("Ghi event ngoài transaction bị từ chối")
    void append_withoutTransaction_isRejected() {
        assertThatThrownBy(() -> outboxWriter.append(TOPIC, "problem-3", newEvent()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private static OutboxChecked newEvent() {
        return new OutboxChecked(UUID.randomUUID(), Instant.now(), 1, "x");
    }

    private static ConsumerRecord<String, String> firstRecordOf(String topic) {
        Map<String, Object> config =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "outbox-test-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "earliest");
        try (KafkaConsumer<String, String> consumer =
                new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(topic));
            long deadline = System.nanoTime() + DELIVERY_TIMEOUT.toNanos();
            List<ConsumerRecord<String, String>> records = new ArrayList<>();
            while (records.isEmpty() && System.nanoTime() < deadline) {
                consumer.poll(Duration.ofMillis(500)).forEach(records::add);
            }
            assertThat(records).as("Kafka record on %s", topic).isNotEmpty();
            return records.get(0);
        }
    }
}
