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
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
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
class JudgeJobKafkaIntegrationTest {

    private static final String REQUESTED = "sqljudge.studio.run-requested";
    private static final String COMPLETED = "sqljudge.studio.run-completed";
    private static final String DEAD_LETTER = REQUESTED + ".dlt";
    private static final String ANSWER_CHECK_REQUESTED = "sqljudge.studio.answer-check-requested";
    private static final String ANSWER_TESTCASE_CHECKED = "sqljudge.studio.answer-testcase-checked";
    private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(60);
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();
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
                    .formatted(VERSION_ID, VARIANT_ID);
    private static final String SUBMISSION_CREATED = "sqljudge.submission.created";
    private static final String JUDGE_PROGRESS = "sqljudge.judge.progress";
    private static final String JUDGE_COMPLETED = "sqljudge.judge.completed";
    private static final String RUN_REQUESTED = "sqljudge.run.requested";
    private static final String RUN_COMPLETED = "sqljudge.run.completed";
    private static final UUID ACCEPTING_REVISION = UUID.randomUUID();
    private static final UUID HIDDEN_MISMATCH_REVISION = UUID.randomUUID();
    private static final String JUDGE_PACKAGE_JSON =
            """
            {"revisionId": "%s", "datasetVersionId": "%s", "datasetChecksum": "abc123",
             "requiresOrder": true, "compareColumnNames": false, "floatTolerance": null,
             "timeLimitMs": 2000, "allowedStatements": ["SELECT", "WITH"],
             "testcases": [
               {"testcaseId": "%s", "index": 1, "variantId": "%s", "isPublic": true,
                "expected": {"columns": ["name"], "rows": [
                  [{"kind": "TEXT", "value": "Kỹ thuật"}], [{"kind": "TEXT", "value": "Kinh doanh"}]]}},
               {"testcaseId": "%s", "index": 2, "variantId": "%s", "isPublic": false,
                "expected": {"columns": ["name"], "rows": %s}}]}
            """;
    private static final String BOTH_ROWS =
            "[[{\"kind\": \"TEXT\", \"value\": \"Kỹ thuật\"}],"
                    + " [{\"kind\": \"TEXT\", \"value\": \"Kinh doanh\"}]]";
    private static final String SECRET_ROW = "[[{\"kind\": \"TEXT\", \"value\": \"Bí mật\"}]]";

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
    @DisplayName("Kiểm tra đáp án gửi mỗi testcase một message về topic riêng, giới hạn 10 MB")
    void answerCheckRequested_validJob_publishesOneMessagePerTestcase()
            throws ExecutionException, InterruptedException {
        String checkId = UUID.randomUUID().toString();
        String request =
                """
                {"eventId": "%s", "occurredAt": "2026-11-03T08:00:00Z", "version": 1,
                 "checkId": "%s", "datasetVersionId": "%s", "datasetChecksum": "abc123",
                 "solutions": {"SQLITE": "SELECT id, name FROM departments ORDER BY id"},
                 "testcases": [{"testcaseId": "%s", "variantId": "%s"},
                               {"testcaseId": "%s", "variantId": "%s"}],
                 "requiresOrder": true, "compareColumnNames": false, "floatTolerance": null,
                 "timeLimitMs": 2000, "allowedStatements": ["SELECT", "WITH"]}
                """
                        .formatted(
                                UUID.randomUUID(),
                                checkId,
                                VERSION_ID,
                                UUID.randomUUID(),
                                VARIANT_ID,
                                UUID.randomUUID(),
                                UUID.randomUUID());

        send(ANSWER_CHECK_REQUESTED, checkId, request);

        List<DocumentContext> checked =
                recordsOf(ANSWER_TESTCASE_CHECKED, checkId, 2).stream()
                        .map(record -> JsonPath.parse(record.value()))
                        .toList();
        DocumentContext first = checked.get(0);
        assertThat(first.read("$.version", Integer.class)).isEqualTo(1);
        assertThat(first.read("$.position", Integer.class)).isEqualTo(1);
        assertThat(first.read("$.testcaseCount", Integer.class)).isEqualTo(2);
        assertThat(first.read("$.expected.columns", List.class)).containsExactly("id", "name");
        assertThat(first.read("$.expected.rows[0][1].kind", String.class)).isEqualTo("TEXT");
        assertThat(first.read("$.expected.rows[0][1].value", String.class)).isEqualTo("Kỹ thuật");
        assertThat(first.read("$.dialects[0].dialect", String.class)).isEqualTo("SQLITE");
        assertThat(first.read("$.dialects[0].verdict", String.class)).isEqualTo("AC");
        assertThat(checked.get(1).read("$.systemError", String.class)).isEqualTo("variant_missing");
        assertThat(maxMessageBytesOf(ANSWER_TESTCASE_CHECKED)).isEqualTo("10485760");
    }

    @Test
    @DisplayName("Bài nộp đúng mọi testcase thì gửi tiến độ từng testcase rồi kết quả AC")
    void submissionCreated_correctSolution_publishesProgressAndAccepted() {
        String submissionId =
                submit(ACCEPTING_REVISION, "SELECT name FROM departments ORDER BY id");

        List<DocumentContext> progress =
                recordsOf(JUDGE_PROGRESS, submissionId, 2).stream()
                        .map(record -> JsonPath.parse(record.value()))
                        .toList();
        assertThat(progress)
                .extracting(event -> event.read("$.judgedTestcases", Integer.class))
                .containsExactly(1, 2);
        assertThat(progress.get(0).read("$.totalTestcases", Integer.class)).isEqualTo(2);
        assertThat(progress.get(0).read("$.attempt", Integer.class)).isEqualTo(1);
        DocumentContext completed =
                JsonPath.parse(firstRecordOf(JUDGE_COMPLETED, submissionId).value());
        assertThat(completed.read("$.submissionId", String.class)).isEqualTo(submissionId);
        assertThat(completed.read("$.version", Integer.class)).isEqualTo(1);
        assertThat(completed.read("$.verdict", String.class)).isEqualTo("AC");
        assertThat(completed.read("$.judgedTestcases", Integer.class)).isEqualTo(2);
        assertThat(completed.read("$.failedIndex", Integer.class)).isNull();
    }

    @Test
    @DisplayName("Sai ở testcase ẩn thì WA kèm số thứ tự, không kèm thông báo hay dữ liệu ẩn")
    void submissionCreated_hiddenMismatch_publishesWrongAnswerWithoutData() {
        String submissionId =
                submit(HIDDEN_MISMATCH_REVISION, "SELECT name FROM departments ORDER BY id");

        String payload = firstRecordOf(JUDGE_COMPLETED, submissionId).value();
        DocumentContext completed = JsonPath.parse(payload);
        assertThat(completed.read("$.verdict", String.class)).isEqualTo("WA");
        assertThat(completed.read("$.failedIndex", Integer.class)).isEqualTo(2);
        assertThat(completed.read("$.failedIsHidden", Boolean.class)).isTrue();
        assertThat(completed.read("$.message", String.class)).isNull();
        assertThat(completed.read("$.diff", Object.class)).isNull();
        assertThat(payload).doesNotContain("Bí mật").doesNotContain("Kinh doanh");
    }

    @Test
    @DisplayName("Không tải được gói chấm thì gửi kết quả SE để submission-service xử lý lại")
    void submissionCreated_unknownRevision_publishesSystemError() {
        String submissionId = submit(UUID.randomUUID(), "SELECT name FROM departments");

        DocumentContext completed =
                JsonPath.parse(firstRecordOf(JUDGE_COMPLETED, submissionId).value());
        assertThat(completed.read("$.verdict", String.class)).isEqualTo("SE");
        assertThat(completed.read("$.judgedTestcases", Integer.class)).isZero();
    }

    @Test
    @DisplayName("Chạy thử trên dataset mẫu rồi gửi bảng kết quả về topic hoàn tất")
    void runRequested_publishesSampleResult() {
        String runId = requestRun(ACCEPTING_REVISION, "SELECT name FROM departments ORDER BY id");

        DocumentContext completed = JsonPath.parse(firstRecordOf(RUN_COMPLETED, runId).value());
        assertThat(completed.read("$.runId", String.class)).isEqualTo(runId);
        assertThat(completed.read("$.version", Integer.class)).isEqualTo(1);
        assertThat(completed.read("$.outcome", String.class)).isEqualTo("OK");
        assertThat(completed.read("$.columns", List.class)).containsExactly("name");
        assertThat(completed.read("$.rows", List.class))
                .containsExactly(List.of("Kỹ thuật"), List.of("Kinh doanh"));
        assertThat(completed.read("$.isTruncated", Boolean.class)).isFalse();
    }

    @Test
    @DisplayName("Chạy thử với bài không tải được gói chấm thì gửi SE")
    void runRequested_unknownRevision_publishesSystemError() {
        String runId = requestRun(UUID.randomUUID(), "SELECT name FROM departments");

        DocumentContext completed = JsonPath.parse(firstRecordOf(RUN_COMPLETED, runId).value());
        assertThat(completed.read("$.outcome", String.class)).isEqualTo("SE");
        assertThat(completed.read("$.rows", List.class)).isEmpty();
    }

    private static String requestRun(UUID revisionId, String code) {
        String runId = UUID.randomUUID().toString();
        send(
                RUN_REQUESTED,
                runId,
                """
                {"eventId": "%s", "occurredAt": "2026-11-11T08:00:00Z", "version": 1,
                 "runId": "%s", "revisionId": "%s", "dialect": "SQLITE", "code": "%s"}
                """
                        .formatted(UUID.randomUUID(), runId, revisionId, code));
        return runId;
    }

    private static String submit(UUID revisionId, String code) {
        String submissionId = UUID.randomUUID().toString();
        send(
                SUBMISSION_CREATED,
                submissionId,
                """
                {"eventId": "%s", "occurredAt": "2026-11-11T08:00:00Z", "version": 1,
                 "submissionId": "%s", "revisionId": "%s", "dialect": "SQLITE",
                 "code": "%s", "attempt": 1}
                """
                        .formatted(UUID.randomUUID(), submissionId, revisionId, code));
        return submissionId;
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
        return recordsOf(topic, key, 1).get(0);
    }

    private static String maxMessageBytesOf(String topic)
            throws ExecutionException, InterruptedException {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        try (Admin admin =
                Admin.create(
                        Map.of(
                                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                                KAFKA.getBootstrapServers()))) {
            Config config = admin.describeConfigs(List.of(resource)).all().get().get(resource);
            return config.get(TopicConfig.MAX_MESSAGE_BYTES_CONFIG).value();
        }
    }

    private static List<ConsumerRecord<String, String>> recordsOf(
            String topic, String key, int count) {
        Map<String, Object> config =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "judge-job-test-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "earliest");
        try (KafkaConsumer<String, String> consumer =
                new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(topic));
            long deadline = System.nanoTime() + DELIVERY_TIMEOUT.toNanos();
            List<ConsumerRecord<String, String>> matches = new ArrayList<>();
            while (matches.size() < count && System.nanoTime() < deadline) {
                consumer.poll(Duration.ofMillis(500))
                        .forEach(
                                record -> {
                                    if (key.equals(record.key())) {
                                        matches.add(record);
                                    }
                                });
            }
            assertThat(matches).as("records with key %s on %s", key, topic).hasSize(count);
            return matches;
        }
    }

    private static HttpServer startProblemService() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    "/internal/dataset-versions/", exchange -> write(exchange, 200, SNAPSHOT_JSON));
            server.createContext(
                    "/internal/problem-revisions/", JudgeJobKafkaIntegrationTest::respondPackage);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void respondPackage(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.contains(ACCEPTING_REVISION.toString())) {
            write(exchange, 200, judgePackage(ACCEPTING_REVISION, BOTH_ROWS));
        } else if (path.contains(HIDDEN_MISMATCH_REVISION.toString())) {
            write(exchange, 200, judgePackage(HIDDEN_MISMATCH_REVISION, SECRET_ROW));
        } else {
            write(exchange, 404, "{\"code\":\"PROBLEM_REVISION_NOT_FOUND\"}");
        }
    }

    private static String judgePackage(UUID revisionId, String hiddenRows) {
        return JUDGE_PACKAGE_JSON.formatted(
                revisionId,
                VERSION_ID,
                UUID.randomUUID(),
                VARIANT_ID,
                UUID.randomUUID(),
                VARIANT_ID,
                hiddenRows);
    }

    private static void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
