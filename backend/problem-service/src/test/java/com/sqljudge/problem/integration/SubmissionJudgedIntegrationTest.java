package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class SubmissionJudgedIntegrationTest extends ContainerSupport {

    private static final String JUDGED = "sqljudge.submission.judged";
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final long DUPLICATE_GRACE_MS = 2000;
    private static final UUID PROBLEM_ID = UUID.fromString("0192f3b0-0000-7000-8000-0000000000c1");
    private static final UUID LEARNER_A = UUID.randomUUID();
    private static final UUID LEARNER_B = UUID.randomUUID();
    private static final UUID LEARNER_C = UUID.randomUUID();

    @ServiceConnection
    static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka:3.9.1")
                    .withEnv("KAFKA_HEAP_OPTS", "-Xms256m -Xmx256m");

    static {
        KAFKA.start();
    }

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void insertProblem() {
        jdbcTemplate.update(
                "INSERT INTO problems (id, slug, title, difficulty, status, author_id,"
                        + " statement_markdown, starter_code, time_limit_ms, requires_order,"
                        + " compare_column_names, float_tolerance, allowed_statements, version,"
                        + " created_at, updated_at) VALUES (UUID_TO_BIN(?), 'bai-thong-ke',"
                        + " 'Bài thống kê', 'EASY', 'DRAFT', UUID_TO_BIN(UUID()), '', '', 2000,"
                        + " FALSE, FALSE, 0.000001, 'SELECT', 0, NOW(6), NOW(6))",
                PROBLEM_ID.toString());
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM processed_events");
        jdbcTemplate.update("DELETE FROM user_problem_progress");
        jdbcTemplate.update("DELETE FROM problem_stats");
        jdbcTemplate.update("DELETE FROM problems");
    }

    @Test
    @DisplayName(
            "Bài đã chấm cập nhật thống kê bài và tiến độ người dùng, event trùng chỉ tính một lần")
    void submissionJudged_updatesStatsAndProgressOnce() throws InterruptedException {
        send(judged(LEARNER_A, "WA", "2026-11-12T08:00:00Z"));
        send(judged(LEARNER_A, "AC", "2026-11-12T08:05:00Z"));
        send(judged(LEARNER_A, "AC", "2026-11-12T08:10:00Z"));
        String learnerBAccepted = judged(LEARNER_B, "AC", "2026-11-12T08:20:00Z");
        send(learnerBAccepted);
        send(learnerBAccepted);
        send(judged(LEARNER_C, "SE", "2026-11-12T08:30:00Z"));

        awaitProcessedEvents(5);
        Thread.sleep(DUPLICATE_GRACE_MS);

        Map<String, Object> stats =
                jdbcTemplate.queryForMap(
                        "SELECT submission_count, accepted_count, solved_user_count"
                                + " FROM problem_stats WHERE problem_id = UUID_TO_BIN(?)",
                        PROBLEM_ID.toString());
        assertThat(stats)
                .containsEntry("submission_count", 4L)
                .containsEntry("accepted_count", 3L)
                .containsEntry("solved_user_count", 2L);
        assertThat(progressOf(LEARNER_A))
                .containsEntry("status", "SOLVED")
                .containsEntry("first_solved_at", "2026-11-12 08:05:00.000000");
        assertThat(progressOf(LEARNER_B)).containsEntry("status", "SOLVED");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM user_problem_progress"
                                        + " WHERE user_id = UUID_TO_BIN(?)",
                                Long.class,
                                LEARNER_C.toString()))
                .isZero();
    }

    private Map<String, Object> progressOf(UUID userId) {
        return jdbcTemplate.queryForMap(
                "SELECT status, DATE_FORMAT(first_solved_at, '%Y-%m-%d %H:%i:%s.%f')"
                        + " AS first_solved_at FROM user_problem_progress"
                        + " WHERE user_id = UUID_TO_BIN(?) AND problem_id = UUID_TO_BIN(?)",
                userId.toString(), PROBLEM_ID.toString());
    }

    private void awaitProcessedEvents(long expected) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        String sql = "SELECT COUNT(*) FROM processed_events";
        while (System.nanoTime() < deadline
                && jdbcTemplate.queryForObject(sql, Long.class) < expected) {
            Thread.sleep(200);
        }
        assertThat(jdbcTemplate.queryForObject(sql, Long.class)).isEqualTo(expected);
    }

    private static String judged(UUID userId, String verdict, String judgedAt) {
        return """
        {"eventId": "%s", "occurredAt": "%s", "version": 1, "submissionId": "%s",
         "userId": "%s", "problemId": "%s", "verdict": "%s", "judgedAt": "%s"}
        """
                .formatted(
                        UUID.randomUUID(),
                        judgedAt,
                        UUID.randomUUID(),
                        userId,
                        PROBLEM_ID,
                        verdict,
                        judgedAt);
    }

    private static void send(String value) {
        String key = JsonPath.read(value, "$.submissionId");
        Map<String, Object> config =
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (KafkaProducer<String, String> producer =
                new KafkaProducer<>(config, new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>(JUDGED, key, value));
            producer.flush();
        }
    }
}
