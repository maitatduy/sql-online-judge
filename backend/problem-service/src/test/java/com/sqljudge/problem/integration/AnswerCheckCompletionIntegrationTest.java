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
import org.junit.jupiter.api.BeforeEach;
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
class AnswerCheckCompletionIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String TESTCASE_CHECKED = "sqljudge.studio.answer-testcase-checked";
    private static final Duration COMPLETION_TIMEOUT = Duration.ofSeconds(60);
    private static final String AGREEING =
            "[{\"dialect\":\"POSTGRESQL\",\"verdict\":\"AC\",\"mismatch\":\"NONE\","
                    + "\"message\":null,\"runtimeMs\":3,\"rowCount\":1},"
                    + "{\"dialect\":\"SQLITE\",\"verdict\":\"AC\",\"mismatch\":\"NONE\","
                    + "\"message\":null,\"runtimeMs\":1,\"rowCount\":1}]";
    private static final String DISAGREEING =
            "[{\"dialect\":\"POSTGRESQL\",\"verdict\":\"AC\",\"mismatch\":\"NONE\","
                    + "\"message\":null,\"runtimeMs\":3,\"rowCount\":1},"
                    + "{\"dialect\":\"SQLITE\",\"verdict\":\"WA\",\"mismatch\":\"ROWS\","
                    + "\"message\":null,\"runtimeMs\":1,\"rowCount\":2}]";
    private static final String EXPECTED =
            "{\"columns\":[\"name\"],\"rows\":[[{\"kind\":\"TEXT\",\"value\":\"Kỹ thuật\"}]]}";

    @ServiceConnection
    static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka:3.9.1")
                    .withEnv("KAFKA_HEAP_OPTS", "-Xms256m -Xmx256m");

    static {
        KAFKA.start();
    }

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private StudioFixtures studio;
    private String checksPath;
    private String checkId;
    private List<String> testcaseIds;

    @BeforeEach
    void startCheck() {
        studio = new StudioFixtures(restTemplate, SETTER);
        String problemId = studio.createProblem("Phòng ban");
        String datasetId = studio.createReadyDataset("Công ty");
        String hiddenVariantId = studio.addVariant(datasetId, "Ẩn", false);
        studio.chooseDataset(problemId, datasetId);
        studio.saveAnswer(problemId, studio.variantIdsOf(datasetId).get(0), hiddenVariantId);
        checksPath = "/api/studio/problems/" + problemId + "/answer-checks";
        checkId =
                JsonPath.parse(studio.send(HttpMethod.POST, checksPath, null).getBody())
                        .read("$.id", String.class);
        String job =
                jdbcTemplate.queryForObject(
                        "SELECT payload FROM outbox_events WHERE message_key = ?",
                        String.class,
                        checkId);
        testcaseIds = JsonPath.parse(job).read("$.testcases[*].testcaseId");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM answer_checks");
        jdbcTemplate.update("UPDATE problems SET draft_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Mọi testcase khớp thì lần kiểm tra đạt và kết quả mong đợi được lưu một lần")
    void testcasesChecked_allAgree_passesAndStoresExpectedResults() throws InterruptedException {
        publish(checkedEvent(testcaseIds.get(0), 1, EXPECTED, AGREEING));
        publish(checkedEvent(testcaseIds.get(0), 1, EXPECTED, DISAGREEING));
        publish(checkedEvent(testcaseIds.get(1), 2, EXPECTED, AGREEING));

        DocumentContext check = awaitStatus("PASSED");
        assertThat(check.read("$.checkedCount", Integer.class)).isEqualTo(2);
        assertThat(check.read("$.completedAt", String.class)).isNotBlank();
        assertThat(check.read("$.testcases[*].position", List.class)).containsExactly(1, 2);
        assertThat(check.read("$.testcases[*].isPublic", List.class)).containsExactly(true, false);
        assertThat(check.read("$.testcases[0].dialects[*].verdict", List.class))
                .containsExactly("AC", "AC");
        assertThat(
                        jdbcTemplate.queryForList(
                                "SELECT row_count FROM expected_results", Integer.class))
                .containsExactly(1, 1);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT rows_json FROM expected_results LIMIT 1", String.class))
                .contains("Kỹ thuật");
    }

    @Test
    @DisplayName("Một dialect lệch hoặc testcase lỗi hệ thống thì lần kiểm tra không đạt")
    void testcasesChecked_disagreement_fails() throws InterruptedException {
        publish(checkedEvent(testcaseIds.get(0), 1, EXPECTED, DISAGREEING));
        publish(systemErrorEvent(testcaseIds.get(1), 2));

        DocumentContext check = awaitStatus("FAILED");
        assertThat(check.read("$.testcases[0].dialects[1].verdict", String.class)).isEqualTo("WA");
        assertThat(check.read("$.testcases[0].dialects[1].mismatch", String.class))
                .isEqualTo("ROWS");
        assertThat(check.read("$.testcases[1].systemError", String.class))
                .isEqualTo("snapshot_unavailable");
    }

    private String checkedEvent(String testcaseId, int position, String expected, String dialects) {
        return """
        {"eventId": "%s", "occurredAt": "2026-11-03T08:00:01Z", "version": 1,
         "checkId": "%s", "testcaseId": "%s", "position": %d, "testcaseCount": 2,
         "systemError": null, "expected": %s, "dialects": %s}
        """
                .formatted(UUID.randomUUID(), checkId, testcaseId, position, expected, dialects);
    }

    private String systemErrorEvent(String testcaseId, int position) {
        return """
        {"eventId": "%s", "occurredAt": "2026-11-03T08:00:01Z", "version": 1,
         "checkId": "%s", "testcaseId": "%s", "position": %d, "testcaseCount": 2,
         "systemError": "snapshot_unavailable", "expected": null, "dialects": []}
        """
                .formatted(UUID.randomUUID(), checkId, testcaseId, position);
    }

    private void publish(String value) {
        Map<String, Object> config =
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (KafkaProducer<String, String> producer =
                new KafkaProducer<>(config, new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>(TESTCASE_CHECKED, checkId, value));
            producer.flush();
        }
    }

    private DocumentContext awaitStatus(String status) throws InterruptedException {
        String path = checksPath + "/" + checkId;
        long deadline = System.nanoTime() + COMPLETION_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            DocumentContext check =
                    JsonPath.parse(studio.send(HttpMethod.GET, path, null).getBody());
            if (status.equals(check.read("$.status", String.class))) {
                return check;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Check " + path + " did not reach " + status + " in time");
    }
}
