package com.sqljudge.submission.integration;

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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(
        properties = {
            "spring.kafka.listener.auto-startup=true",
            "spring.kafka.admin.auto-create=true"
        })
@ActiveProfiles("test")
class JudgeResultIntegrationTest extends ContainerSupport {

    private static final String PROGRESS = "sqljudge.judge.progress";
    private static final String COMPLETED = "sqljudge.judge.completed";
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final long DUPLICATE_GRACE_MS = 2000;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROBLEM_ID = UUID.randomUUID();

    @ServiceConnection
    static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka:3.9.1")
                    .withEnv("KAFKA_HEAP_OPTS", "-Xms256m -Xmx256m");

    static {
        KAFKA.start();
    }

    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM submissions");
        jdbcTemplate.update("DELETE FROM code_runs");
    }

    @Test
    @DisplayName("Kết quả chạy thử được ghi vào lần chạy; kết quả gửi trùng bị bỏ qua")
    void runCompleted_storesResultOnce() throws InterruptedException {
        String runId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO code_runs (id, user_id, problem_id, problem_revision_id, dialect,"
                        + " code, status, version, created_at, updated_at) VALUES"
                        + " (UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(UUID()),"
                        + " UUID_TO_BIN(UUID()), 'POSTGRESQL', 'SELECT 1', 'PENDING', 0, NOW(6),"
                        + " NOW(6))",
                runId,
                USER_ID.toString());

        send("sqljudge.run.completed", runId, runCompleted(runId, "OK", "[[\"Kỹ thuật\"]]"));
        awaitRunStatus(runId);
        send("sqljudge.run.completed", runId, runCompleted(runId, "SE", "[]"));
        Thread.sleep(DUPLICATE_GRACE_MS);

        Map<String, Object> stored =
                jdbcTemplate.queryForMap(
                        "SELECT outcome, runtime_ms, result_json, completed_at FROM code_runs"
                                + " WHERE id = UUID_TO_BIN(?)",
                        runId);
        assertThat(stored).containsEntry("outcome", "OK").containsEntry("runtime_ms", 4);
        assertThat(stored.get("completed_at")).isNotNull();
        DocumentContext result = JsonPath.parse(String.valueOf(stored.get("result_json")));
        assertThat(result.read("$.columns", List.class)).containsExactly("name");
        assertThat(result.read("$.rows[0][0]", String.class)).isEqualTo("Kỹ thuật");
        assertThat(result.read("$.isTruncated", Boolean.class)).isFalse();
    }

    private void awaitRunStatus(String runId) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        String sql = "SELECT status FROM code_runs WHERE id = UUID_TO_BIN(?)";
        while (System.nanoTime() < deadline
                && !"COMPLETED".equals(jdbcTemplate.queryForObject(sql, String.class, runId))) {
            Thread.sleep(200);
        }
        assertThat(jdbcTemplate.queryForObject(sql, String.class, runId)).isEqualTo("COMPLETED");
    }

    private static String runCompleted(String runId, String outcome, String rows) {
        return """
        {"eventId": "%s", "occurredAt": "2026-11-11T08:00:02Z", "version": 1,
         "runId": "%s", "outcome": "%s", "columns": ["name"], "rows": %s,
         "isTruncated": false, "runtimeMs": 4, "message": null}
        """
                .formatted(UUID.randomUUID(), runId, outcome, rows);
    }

    @Test
    @DisplayName("Tiến độ rồi kết quả chấm được ghi vào bài nộp và phát event bài đã chấm")
    void progressThenCompleted_storesVerdictAndQueuesJudgedEvent() throws InterruptedException {
        String id = storeSubmission();

        send(PROGRESS, id, progress(id, 1, 1));
        awaitColumn(id, "judged_testcases", 1);
        assertThat(column(id, "status")).isEqualTo("JUDGING");
        send(COMPLETED, id, completed(id, 1, "WA", "\"failedIndex\": 2, \"failedIsHidden\": true"));

        awaitColumn(id, "status", "COMPLETED");
        Map<String, Object> stored =
                jdbcTemplate.queryForMap(
                        "SELECT verdict, runtime_ms, first_failed_index, first_failed_is_hidden,"
                                + " judged_testcases, judged_at FROM submissions"
                                + " WHERE id = UUID_TO_BIN(?)",
                        id);
        assertThat(stored)
                .containsEntry("verdict", "WA")
                .containsEntry("runtime_ms", 7)
                .containsEntry("first_failed_index", 2)
                .containsEntry("first_failed_is_hidden", true)
                .containsEntry("judged_testcases", 2);
        assertThat(stored.get("judged_at")).isNotNull();
        DocumentContext judged = JsonPath.parse(outboxPayload("sqljudge.submission.judged", id));
        assertThat(judged.read("$.submissionId", String.class)).isEqualTo(id);
        assertThat(judged.read("$.userId", String.class)).isEqualTo(USER_ID.toString());
        assertThat(judged.read("$.problemId", String.class)).isEqualTo(PROBLEM_ID.toString());
        assertThat(judged.read("$.verdict", String.class)).isEqualTo("WA");
        assertThat(judged.read("$.version", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Lỗi máy chấm được gửi lại job với lần chấm mới; kết quả cũ hay trùng bị bỏ qua")
    void systemError_requeuesWithNextAttemptAndIgnoresStaleResults() throws InterruptedException {
        String id = storeSubmission();

        send(COMPLETED, id, completed(id, 1, "SE", "\"failedIndex\": null"));
        awaitColumn(id, "attempts", 2);
        assertThat(column(id, "status")).isEqualTo("PENDING");
        DocumentContext requeued = JsonPath.parse(outboxPayload("sqljudge.submission.created", id));
        assertThat(requeued.read("$.attempt", Integer.class)).isEqualTo(2);
        assertThat(requeued.read("$.code", String.class)).isEqualTo("SELECT name FROM departments");

        send(COMPLETED, id, completed(id, 1, "AC", "\"failedIndex\": null"));
        send(COMPLETED, id, completed(id, 2, "AC", "\"failedIndex\": null"));
        awaitColumn(id, "status", "COMPLETED");

        assertThat(column(id, "verdict")).isEqualTo("AC");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM outbox_events WHERE topic = ?",
                                Long.class,
                                "sqljudge.submission.judged"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Kết quả sai ở testcase công khai lưu kèm diff")
    void completedOnPublicTestcase_storesDiff() throws InterruptedException {
        String id = storeSubmission();
        String diff =
                """
                "diff": {"reason": "ROWS", "expectedColumns": ["name"],
                 "actualColumns": ["name"], "expectedRowCount": 1, "actualRowCount": 1,
                 "missingRows": [[{"kind": "TEXT", "value": "Kỹ thuật"}]],
                 "extraRows": [[{"kind": "NULL", "value": null}]], "orderMismatch": null}
                """;

        send(
                COMPLETED,
                id,
                completed(id, 1, "WA", "\"failedIndex\": 1, \"failedIsHidden\": false, " + diff));
        awaitColumn(id, "status", "COMPLETED");

        DocumentContext stored = JsonPath.parse(String.valueOf(column(id, "diff_json")));
        assertThat(stored.read("$.reason", String.class)).isEqualTo("ROWS");
        assertThat(stored.read("$.missingRows[0][0].value", String.class)).isEqualTo("Kỹ thuật");
        assertThat(stored.read("$.extraRows[0][0].kind", String.class)).isEqualTo("NULL");
    }

    private String storeSubmission() {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO submissions (id, user_id, problem_id, problem_revision_id, dialect,"
                        + " code, status, judged_testcases, total_testcases, attempts, version,"
                        + " created_at, updated_at) VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(UUID()), 'POSTGRESQL',"
                        + " 'SELECT name FROM departments', 'PENDING', 0, 2, 1, 0, NOW(6), NOW(6))",
                id,
                USER_ID.toString(),
                PROBLEM_ID.toString());
        return id;
    }

    private static String progress(String id, int attempt, int judged) {
        return """
        {"eventId": "%s", "occurredAt": "2026-11-11T08:00:00Z", "version": 1,
         "submissionId": "%s", "attempt": %d, "judgedTestcases": %d, "totalTestcases": 2}
        """
                .formatted(UUID.randomUUID(), id, attempt, judged);
    }

    private static String completed(String id, int attempt, String verdict, String failure) {
        return """
        {"eventId": "%s", "occurredAt": "2026-11-11T08:00:01Z", "version": 1,
         "submissionId": "%s", "attempt": %d, "verdict": "%s", "runtimeMs": 7, %s,
         "message": null, "judgedTestcases": 2, "totalTestcases": 2}
        """
                .formatted(UUID.randomUUID(), id, attempt, verdict, failure);
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

    private Object column(String id, String name) {
        List<Object> values =
                jdbcTemplate.queryForList(
                        "SELECT " + name + " FROM submissions WHERE id = UUID_TO_BIN(?)",
                        Object.class,
                        id);
        return values.isEmpty() ? null : values.get(0);
    }

    private void awaitColumn(String id, String name, Object expected) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (String.valueOf(expected).equals(String.valueOf(column(id, name)))) {
                return;
            }
            Thread.sleep(200);
        }
        assertThat(column(id, name)).as(name).hasToString(String.valueOf(expected));
    }

    private String outboxPayload(String topic, String key) {
        return jdbcTemplate.queryForObject(
                "SELECT payload FROM outbox_events WHERE topic = ? AND message_key = ?",
                String.class,
                topic,
                key);
    }
}
