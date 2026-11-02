package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.kafka.listener.auto-startup=true",
            "spring.kafka.admin.auto-create=true"
        })
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class StudioRunCompletionIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String RUN_COMPLETED = "sqljudge.studio.run-completed";
    private static final Duration COMPLETION_TIMEOUT = Duration.ofSeconds(60);

    @ServiceConnection
    static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka:3.9.1")
                    .withEnv("KAFKA_HEAP_OPTS", "-Xms256m -Xmx256m");

    static {
        KAFKA.start();
    }

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM studio_runs");
        jdbcTemplate.update("UPDATE problems SET draft_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Kết quả máy chấm gửi về được lưu, nhận trùng event thì giữ nguyên kết quả đầu")
    void runCompleted_storesResultOnce() throws InterruptedException {
        StudioFixtures studio = new StudioFixtures(restTemplate, SETTER);
        String problemId = studio.createProblem("Phòng ban");
        studio.chooseDataset(problemId, studio.createReadyDataset("Công ty"));
        String runPath = "/api/studio/problems/" + problemId + "/runs";
        String runId =
                JsonPath.parse(
                                studio.send(
                                                HttpMethod.POST,
                                                runPath,
                                                "{\"dialect\":\"SQLITE\",\"sql\":\"SELECT name FROM"
                                                        + " departments\"}")
                                        .getBody())
                        .read("$.id", String.class);

        publish(runId, completedEvent(runId, "Kỹ thuật"));
        publish(runId, completedEvent(runId, "Bản trùng"));

        DocumentContext run = awaitCompleted(studio, runPath + "/" + runId);
        assertThat(run.read("$.completedAt", String.class)).isNotBlank();
        assertThat(run.read("$.rejection", Object.class)).isNull();
        assertThat(run.read("$.variants[0].variantName", String.class)).isEqualTo("Mẫu");
        assertThat(run.read("$.variants[0].status", String.class)).isEqualTo("SUCCEEDED");
        assertThat(run.read("$.variants[0].columns", List.class)).containsExactly("name");
        assertThat(run.read("$.variants[0].rows", List.class)).containsExactly(List.of("Kỹ thuật"));
        Thread.sleep(2_000);
        assertThat(
                        JsonPath.parse(
                                        studio.send(HttpMethod.GET, runPath + "/" + runId, null)
                                                .getBody())
                                .read("$.variants[0].rows", List.class))
                .containsExactly(List.of("Kỹ thuật"));
    }

    private static String completedEvent(String runId, String departmentName) {
        return """
        {"eventId": "%s", "occurredAt": "2026-11-02T08:00:01Z", "version": 1,
         "runId": "%s", "dialect": "SQLITE", "rejection": null, "systemError": null,
         "variants": [{"variantId": "%s", "variantName": "Mẫu", "isPublic": true,
           "status": "SUCCEEDED", "columns": ["name"], "rows": [["%s"]],
           "rowCount": 1, "truncated": false, "runtimeMs": 3, "message": null}]}
        """
                .formatted(UUID.randomUUID(), runId, UUID.randomUUID(), departmentName);
    }

    private static void publish(String key, String value) {
        Map<String, Object> config =
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (KafkaProducer<String, String> producer =
                new KafkaProducer<>(config, new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>(RUN_COMPLETED, key, value));
            producer.flush();
        }
    }

    private static DocumentContext awaitCompleted(StudioFixtures studio, String path)
            throws InterruptedException {
        long deadline = System.nanoTime() + COMPLETION_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            DocumentContext run = JsonPath.parse(studio.send(HttpMethod.GET, path, null).getBody());
            if ("COMPLETED".equals(run.read("$.status", String.class))) {
                return run;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Run " + path + " did not complete in time");
    }
}
