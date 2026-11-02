package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class StudioRunApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String RUN_REQUESTED = "sqljudge.studio.run-requested";
    private static final String QUERY =
            "{\"dialect\":\"POSTGRESQL\",\"sql\":\"SELECT name FROM departments\"}";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private StudioFixtures studio;
    private String problemId;

    @BeforeEach
    void createProblem() {
        studio = new StudioFixtures(restTemplate, SETTER);
        problemId = studio.createProblem("Phòng ban");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM studio_runs");
        jdbcTemplate.update("UPDATE problems SET draft_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Chạy thử trả 202, lưu lần chạy đang chờ và ghi job vào outbox cùng transaction")
    void createRun_problemWithDataset_isAcceptedAndQueued() {
        studio.chooseDataset(problemId, studio.createReadyDataset("Công ty"));

        ResponseEntity<String> response = studio.send(HttpMethod.POST, runsPath(), QUERY);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        DocumentContext run = JsonPath.parse(response.getBody());
        String runId = run.read("$.id", String.class);
        assertThat(run.read("$.status", String.class)).isEqualTo("PENDING");
        assertThat(run.read("$.dialect", String.class)).isEqualTo("POSTGRESQL");
        assertThat(response.getHeaders().getLocation()).hasPath(runsPath() + "/" + runId);
        Map<String, Object> outbox =
                jdbcTemplate.queryForMap("SELECT topic, message_key, payload FROM outbox_events");
        assertThat(outbox.get("topic")).isEqualTo(RUN_REQUESTED);
        assertThat(outbox.get("message_key")).isEqualTo(runId);
        DocumentContext job = JsonPath.parse((String) outbox.get("payload"));
        assertThat(job.read("$.runId", String.class)).isEqualTo(runId);
        assertThat(job.read("$.dialect", String.class)).isEqualTo("POSTGRESQL");
        assertThat(job.read("$.sql", String.class)).isEqualTo("SELECT name FROM departments");
        assertThat(job.read("$.timeLimitMs", Integer.class)).isEqualTo(2000);
        assertThat(job.read("$.datasetChecksum", String.class)).hasSize(64);
        assertThat(job.read("$.version", Integer.class)).isEqualTo(1);
        DocumentContext fetched =
                JsonPath.parse(
                        studio.send(HttpMethod.GET, runsPath() + "/" + runId, null).getBody());
        assertThat(fetched.read("$.status", String.class)).isEqualTo("PENDING");
        assertThat(fetched.read("$.sql", String.class)).isEqualTo("SELECT name FROM departments");
    }

    @Test
    @DisplayName("Bài chưa chọn database thì không chạy thử được: 422 PROBLEM_DATASET_MISSING")
    void createRun_problemWithoutDataset_isRejected() {
        ResponseEntity<String> response = studio.send(HttpMethod.POST, runsPath(), QUERY);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("PROBLEM_DATASET_MISSING");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events", Long.class))
                .isZero();
    }

    @Test
    @DisplayName("SQL trống trả 400, học viên nhận 403, người ra đề khác nhận 404")
    void createRun_invalidRequestOrRole_isRejected() {
        ResponseEntity<String> blank =
                studio.send(HttpMethod.POST, runsPath(), "{\"dialect\":\"MYSQL\",\"sql\":\"  \"}");

        assertThat(blank.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(blank.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("sql");
        assertThat(
                        studio.send(HttpMethod.POST, runsPath(), QUERY, TestJwts.learnerToken())
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(studio.send(HttpMethod.POST, runsPath(), QUERY, OTHER_SETTER).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Lần chạy không tồn tại hoặc thuộc bài khác trả 404 STUDIO_RUN_NOT_FOUND")
    void getRun_unknownRun_isNotFound() {
        ResponseEntity<String> response =
                studio.send(HttpMethod.GET, runsPath() + "/" + UUID.randomUUID(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("STUDIO_RUN_NOT_FOUND");
    }

    private String runsPath() {
        return "/api/studio/problems/" + problemId + "/runs";
    }
}
