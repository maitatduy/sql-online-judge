package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
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
class AnswerCheckApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String CHECK_REQUESTED = "sqljudge.studio.answer-check-requested";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private StudioFixtures studio;
    private String problemId;
    private String sampleVariantId;
    private String hiddenVariantId;

    @BeforeEach
    void createProblemWithDataset() {
        studio = new StudioFixtures(restTemplate, SETTER);
        problemId = studio.createProblem("Phòng ban");
        String datasetId = studio.createReadyDataset("Công ty");
        hiddenVariantId = studio.addVariant(datasetId, "Ẩn", false);
        sampleVariantId = studio.variantIdsOf(datasetId).get(0);
        studio.chooseDataset(problemId, datasetId);
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
    @DisplayName("Kiểm tra đáp án trả 202, lưu lần kiểm tra đang chờ và ghi job vào outbox")
    void startCheck_completeAnswer_isAcceptedAndQueued() {
        studio.saveAnswer(problemId, sampleVariantId, hiddenVariantId);

        ResponseEntity<String> response = studio.send(HttpMethod.POST, checksPath(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        DocumentContext check = JsonPath.parse(response.getBody());
        String checkId = check.read("$.id", String.class);
        assertThat(check.read("$.status", String.class)).isEqualTo("PENDING");
        assertThat(check.read("$.testcaseCount", Integer.class)).isEqualTo(2);
        assertThat(check.read("$.checkedCount", Integer.class)).isZero();
        assertThat(response.getHeaders().getLocation()).hasPath(checksPath() + "/" + checkId);
        Map<String, Object> outbox =
                jdbcTemplate.queryForMap("SELECT topic, message_key, payload FROM outbox_events");
        assertThat(outbox.get("topic")).isEqualTo(CHECK_REQUESTED);
        assertThat(outbox.get("message_key")).isEqualTo(checkId);
        DocumentContext job = JsonPath.parse((String) outbox.get("payload"));
        assertThat(job.read("$.checkId", String.class)).isEqualTo(checkId);
        assertThat(job.read("$.version", Integer.class)).isEqualTo(1);
        assertThat(job.read("$.datasetChecksum", String.class)).hasSize(64);
        assertThat(job.read("$.solutions.POSTGRESQL", String.class))
                .isEqualTo("SELECT name FROM departments");
        assertThat(job.read("$.solutions.SQLITE", String.class))
                .isEqualTo("SELECT name FROM departments;");
        assertThat(job.read("$.testcases[*].variantId", List.class))
                .containsExactly(sampleVariantId, hiddenVariantId);
        assertThat(job.read("$.requiresOrder", Boolean.class)).isTrue();
        assertThat(job.read("$.floatTolerance", Double.class)).isEqualTo(0.01);
        assertThat(job.read("$.timeLimitMs", Integer.class)).isEqualTo(2000);
        assertThat(job.read("$.allowedStatements", List.class))
                .containsExactlyInAnyOrder("SELECT", "WITH");
        DocumentContext fetched =
                JsonPath.parse(
                        studio.send(HttpMethod.GET, checksPath() + "/" + checkId, null).getBody());
        assertThat(fetched.read("$.status", String.class)).isEqualTo("PENDING");
        assertThat(fetched.read("$.testcases", List.class)).isEmpty();
    }

    @Test
    @DisplayName("Chưa có đáp án hoặc testcase thì không kiểm tra được: 422")
    void startCheck_incompleteAnswer_isRejected() {
        ResponseEntity<String> response = studio.send(HttpMethod.POST, checksPath(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("PROBLEM_ANSWER_INCOMPLETE");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events", Long.class))
                .isZero();
    }

    @Test
    @DisplayName("Học viên nhận 403, người ra đề khác nhận 404, lần kiểm tra lạ nhận 404")
    void startCheck_wrongRoleOrOwner_isRejected() {
        studio.saveAnswer(problemId, sampleVariantId);

        assertThat(
                        studio.send(HttpMethod.POST, checksPath(), null, TestJwts.learnerToken())
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(studio.send(HttpMethod.POST, checksPath(), null, OTHER_SETTER).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        ResponseEntity<String> unknown =
                studio.send(HttpMethod.GET, checksPath() + "/" + UUID.randomUUID(), null);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(unknown.getBody()).read("$.code", String.class))
                .isEqualTo("ANSWER_CHECK_NOT_FOUND");
    }

    private String checksPath() {
        return "/api/studio/problems/" + problemId + "/answer-checks";
    }
}
