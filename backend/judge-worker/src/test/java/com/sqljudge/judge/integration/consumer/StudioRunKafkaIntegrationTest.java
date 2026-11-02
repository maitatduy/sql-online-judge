package com.sqljudge.judge.integration.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.kafka.listener.auto-startup=true",
            "spring.kafka.admin.auto-create=true"
        })
@ActiveProfiles("test")
class StudioRunKafkaIntegrationTest {

    private static final String REQUESTED = "sqljudge.studio.run-requested";
    private static final String COMPLETED = "sqljudge.studio.run-completed";
    private static final String DEAD_LETTER = REQUESTED + ".dlt";
    private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(60);
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final String SNAPSHOT_JSON =
            """
            {"versionId": "%s", "checksum": "abc123",
             "tables": [{"name": "departments", "columns": [
               {"name": "id", "type": "INTEGER", "isPrimaryKey": true, "isNullable": false},
               {"name": "name", "type": "TEXT", "isPrimaryKey": false, "isNullable": false}]}],
             "variants": [{"id": "%s", "name": "Mẫu", "isPublic": true,
               "tables": [{"tableName": "departments",
                           "rows": [["2", "Kinh doanh"], ["1", "Kỹ thuật"]]}]}]}
            """
                    .formatted(VERSION_ID, UUID.randomUUID());

    @ServiceConnection
    static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka:3.9.1")
                    .withEnv("KAFKA_HEAP_OPTS", "-Xms256m -Xmx256m");

    private static final HttpServer PROBLEM_SERVICE = startProblemService();

    static {
        KAFKA.start();
    }

    @DynamicPropertySource
    static void problemServiceUrl(DynamicPropertyRegistry registry) {
        registry.add(
                "sqljudge.problem-service-url",
                () -> "http://localhost:" + PROBLEM_SERVICE.getAddress().getPort());
    }

    @AfterAll
    static void stopProblemService() {
        PROBLEM_SERVICE.stop(0);
    }

    @Test
    @DisplayName("Nhận job chạy thử từ Kafka, chạy trên SQLite và gửi kết quả về topic hoàn tất")
    void runRequested_validJob_publishesCompletedResult() {
        String runId = UUID.randomUUID().toString();
        String request =
                """
                {"eventId": "%s", "occurredAt": "2026-11-02T08:00:00Z", "version": 1,
                 "runId": "%s", "datasetVersionId": "%s", "datasetChecksum": "abc123",
                 "dialect": "SQLITE", "sql": "SELECT name FROM departments ORDER BY id",
                 "timeLimitMs": 2000}
                """
                        .formatted(UUID.randomUUID(), runId, VERSION_ID);

        send(REQUESTED, runId, request);

        ConsumerRecord<String, String> record = firstRecordOf(COMPLETED, runId);
        DocumentContext completed = JsonPath.parse(record.value());
        assertThat(completed.read("$.runId", String.class)).isEqualTo(runId);
        assertThat(completed.read("$.version", Integer.class)).isEqualTo(1);
        assertThat(completed.read("$.eventId", String.class)).isNotBlank();
        assertThat(completed.read("$.dialect", String.class)).isEqualTo("SQLITE");
        assertThat(completed.read("$.variants[0].status", String.class)).isEqualTo("SUCCEEDED");
        assertThat(completed.read("$.variants[0].columns", List.class)).containsExactly("name");
        assertThat(completed.read("$.variants[0].rows", List.class))
                .containsExactly(List.of("Kỹ thuật"), List.of("Kinh doanh"));
    }

    @Test
    @DisplayName("Message hỏng được đẩy sang dead letter topic, không làm kẹt partition")
    void runRequested_malformedJob_goesToDeadLetterTopic() {
        String key = UUID.randomUUID().toString();

        send(REQUESTED, key, "đây không phải JSON");

        assertThat(firstRecordOf(DEAD_LETTER, key).value()).isEqualTo("đây không phải JSON");
    }

    private static void send(String topic, String key, String value) {
        Map<String, Object> config =
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (KafkaProducer<String, String> producer =
                new KafkaProducer<>(config, new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>(topic, key, value));
            producer.flush();
        }
    }

    private static ConsumerRecord<String, String> firstRecordOf(String topic, String key) {
        Map<String, Object> config =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "studio-run-test-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "earliest");
        try (KafkaConsumer<String, String> consumer =
                new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(topic));
            long deadline = System.nanoTime() + DELIVERY_TIMEOUT.toNanos();
            List<ConsumerRecord<String, String>> matches = new ArrayList<>();
            while (matches.isEmpty() && System.nanoTime() < deadline) {
                consumer.poll(Duration.ofMillis(500))
                        .forEach(
                                record -> {
                                    if (key.equals(record.key())) {
                                        matches.add(record);
                                    }
                                });
            }
            assertThat(matches).as("record with key %s on %s", key, topic).isNotEmpty();
            return matches.get(0);
        }
    }

    private static HttpServer startProblemService() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    "/internal/dataset-versions/", StudioRunKafkaIntegrationTest::respond);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        byte[] body = SNAPSHOT_JSON.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
