package com.sqljudge.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.learning.event.LessonRunCompletedConsumer;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class LessonRunApiIntegrationTest extends ContainerSupport {

    private static final String SQL = "SELECT name FROM departments";
    private static final UUID OTHER_LEARNER_ID =
            UUID.fromString("0192f3a0-0000-7000-8000-000000000002");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LessonRunCompletedConsumer consumer;

    private CourseFixtures fixtures;
    private String cellKey;

    @BeforeEach
    void createCell() {
        jdbcTemplate.update("DELETE FROM lesson_runs");
        jdbcTemplate.update("DELETE FROM outbox_events");
        fixtures = new CourseFixtures(jdbcTemplate);
        fixtures.clear();
        String revisionId = fixtures.publishedCourse("sql-co-ban", "EASY", "2026-11-01 08:00:00");
        String lessonKey = fixtures.lesson(fixtures.chapter(revisionId, "Chương", 1), "bai", 1);
        cellKey = fixtures.cell(lessonKey, 1, "SELECT * FROM departments;");
    }

    @Test
    @DisplayName("Chạy ô hợp lệ trả 202, lưu lần chạy đang chờ và ghi job kèm dataset của ô")
    void start_validCell_isAcceptedAndQueued() throws Exception {
        ResponseEntity<String> response = start(cellKey, SQL, TestJwts.learnerToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String runId = objectMapper.readTree(response.getBody()).get("runId").asText();
        Map<String, Object> outbox =
                jdbcTemplate.queryForMap("SELECT topic, message_key, payload FROM outbox_events");
        assertThat(outbox.get("topic")).isEqualTo("sqljudge.lesson.run-requested");
        assertThat(outbox.get("message_key")).isEqualTo(runId);
        JsonNode job = objectMapper.readTree((String) outbox.get("payload"));
        Map<String, Object> cell =
                jdbcTemplate.queryForMap(
                        "SELECT BIN_TO_UUID(dataset_version_id) AS version_id,"
                                + " BIN_TO_UUID(dataset_variant_id) AS variant_id"
                                + " FROM lesson_cells WHERE cell_key = UUID_TO_BIN(?)",
                        cellKey);
        assertThat(job.get("runId").asText()).isEqualTo(runId);
        assertThat(job.get("datasetVersionId").asText()).isEqualTo(cell.get("version_id"));
        assertThat(job.get("datasetVariantId").asText()).isEqualTo(cell.get("variant_id"));
        assertThat(job.get("datasetChecksum").asText()).isEqualTo("abc123");
        assertThat(job.get("dialect").asText()).isEqualTo("POSTGRESQL");
        assertThat(job.get("sql").asText()).isEqualTo(SQL);
        assertThat(job.get("timeLimitMs").asInt()).isEqualTo(2000);

        JsonNode pending = getRun(runId, TestJwts.learnerToken());
        assertThat(pending.get("status").asText()).isEqualTo("PENDING");
        assertThat(pending.get("result").isNull()).isTrue();
    }

    @Test
    @DisplayName("Kết quả từ máy chấm được lưu một lần, lần gửi lại bị bỏ qua")
    void complete_storesResultOnce() throws Exception {
        String runId =
                objectMapper
                        .readTree(start(cellKey, SQL, TestJwts.learnerToken()).getBody())
                        .get("runId")
                        .asText();

        consumer.onLessonRunCompleted(completed(runId, "OK", "[[\"Kỹ thuật\"]]", null));
        consumer.onLessonRunCompleted(completed(runId, "RE", "[]", "lỗi sau"));

        JsonNode run = getRun(runId, TestJwts.learnerToken());
        assertThat(run.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(run.get("outcome").asText()).isEqualTo("OK");
        assertThat(run.get("runtimeMs").asInt()).isEqualTo(6);
        assertThat(run.get("result").get("columns").get(0).asText()).isEqualTo("name");
        assertThat(run.get("result").get("rows").get(0).get(0).asText()).isEqualTo("Kỹ thuật");
        assertThat(run.get("errorMessage").isNull()).isTrue();
    }

    @Test
    @DisplayName("SQL trống hoặc quá 64 KB trả 400 và không tạo lần chạy")
    void start_invalidSql_isRejected() {
        ResponseEntity<String> blank = start(cellKey, " ", TestJwts.learnerToken());
        ResponseEntity<String> tooLong =
                start(cellKey, "S".repeat(65_537), TestJwts.learnerToken());

        assertThat(blank.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(blank.getBody()).contains("\"code\":\"VALIDATION_FAILED\"");
        assertThat(tooLong.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lesson_runs", Long.class))
                .isZero();
    }

    @Test
    @DisplayName("Ô không tồn tại hoặc thuộc khoá chưa xuất bản trả 404, khách trả 401")
    void start_hiddenOrMissingCell_isNotFound() {
        String draftRevision = fixtures.course("ban-nhap", "DRAFT", "EASY", null, false);
        String draftLesson = fixtures.lesson(fixtures.chapter(draftRevision, "Chương", 1), "x", 1);
        String draftCell = fixtures.cell(draftLesson, 1, "SELECT 1;");

        for (String key : new String[] {draftCell, UUID.randomUUID().toString()}) {
            ResponseEntity<String> response = start(key, SQL, TestJwts.learnerToken());
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).contains("\"code\":\"LESSON_CELL_NOT_FOUND\"");
        }
        assertThat(
                        restTemplate
                                .postForEntity(
                                        "/api/lesson-cells/" + cellKey + "/runs",
                                        null,
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Lần chạy của người khác hoặc không tồn tại trả 404")
    void get_otherLearnersRun_isNotFound() throws Exception {
        String runId =
                objectMapper
                        .readTree(start(cellKey, SQL, TestJwts.learnerToken()).getBody())
                        .get("runId")
                        .asText();

        ResponseEntity<String> response =
                exchange(
                        HttpMethod.GET,
                        "/api/lesson-runs/" + runId,
                        null,
                        TestJwts.token(OTHER_LEARNER_ID, "LEARNER"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("\"code\":\"LESSON_RUN_NOT_FOUND\"");
    }

    private ResponseEntity<String> start(String key, String sql, String token) {
        return exchange(
                HttpMethod.POST,
                "/api/lesson-cells/" + key + "/runs",
                "{\"sql\":" + quoted(sql) + "}",
                token);
    }

    private JsonNode getRun(String runId, String token) throws Exception {
        ResponseEntity<String> response =
                exchange(HttpMethod.GET, "/api/lesson-runs/" + runId, null, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private ResponseEntity<String> exchange(
            HttpMethod method, String path, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private String quoted(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String completed(String runId, String outcome, String rows, String message) {
        return """
        {"eventId": "%s", "occurredAt": "2026-11-15T08:00:00Z", "version": 1,
         "runId": "%s", "outcome": "%s", "columns": ["name"], "rows": %s,
         "isTruncated": false, "runtimeMs": 6, "message": %s}
        """
                .formatted(
                        UUID.randomUUID(),
                        runId,
                        outcome,
                        rows,
                        message == null ? "null" : "\"" + message + "\"");
    }
}
