package com.sqljudge.contest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class ContestProblemAccessApiIntegrationTest extends ContainerSupport {

    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final UUID CONTESTANT_ID = UUID.randomUUID();
    private static final String CONTESTANT = TestJwts.token(CONTESTANT_ID, "LEARNER");
    private static final String OUTSIDER = TestJwts.token(UUID.randomUUID(), "LEARNER");
    private static final String PROBLEMS = "/api/contests/sql-cup/problems";
    private static final UUID FIRST_PROBLEM_ID = UUID.randomUUID();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private String contestId;

    @DynamicPropertySource
    static void problemService(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.problem-service-url", ProblemServiceStub::url);
    }

    @BeforeEach
    void createContestWithProblems() throws Exception {
        jdbcTemplate.update("DELETE FROM contest_rating_changes");
        jdbcTemplate.update("DELETE FROM contest_standings");
        jdbcTemplate.update("DELETE FROM contest_cells");
        jdbcTemplate.update("DELETE FROM contest_attempts");
        jdbcTemplate.update("DELETE FROM contest_registrations");
        jdbcTemplate.update("DELETE FROM contest_problems");
        jdbcTemplate.update("DELETE FROM contests");
        Map<String, Object> contest =
                Map.of(
                        "slug",
                        "sql-cup",
                        "title",
                        "SQL Cup",
                        "startAt",
                        Instant.now().plus(Duration.ofDays(1)).toString(),
                        "durationMinutes",
                        90,
                        "freezeMinutes",
                        30,
                        "penaltyMinutes",
                        20,
                        "isRated",
                        true);
        contestId =
                objectMapper
                        .readTree(
                                send(HttpMethod.POST, "/api/admin/contests", ADMIN, contest)
                                        .getBody())
                        .get("id")
                        .asText();
        ProblemServiceStub.reset();
        ProblemServiceStub.statement(FIRST_PROBLEM_ID, "Ghép phòng ban");
        insertProblem(0, FIRST_PROBLEM_ID, "Ghép phòng ban");
        insertProblem(1, UUID.randomUUID(), "Xếp hạng lương");
        jdbcTemplate.update(
                "INSERT INTO contest_registrations (id, contest_id, user_id, username, created_at,"
                        + " updated_at) VALUES (UUID_TO_BIN(UUID()), UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), 'thi.sinh', NOW(6), NOW(6))",
                contestId,
                CONTESTANT_ID.toString());
    }

    @Test
    @DisplayName("Contest chưa bắt đầu thì kể cả người đã đăng ký cũng chưa thấy bài")
    void upcomingContest_hidesProblems() throws Exception {
        ResponseEntity<String> response = send(HttpMethod.GET, PROBLEMS, CONTESTANT, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(body(response).get("code").asText()).isEqualTo("CONTEST_NOT_STARTED");
        assertThat(response.getBody()).doesNotContain("Ghép phòng ban");
    }

    @Test
    @DisplayName("Đang thi thì người đã đăng ký thấy bài theo chữ cái, người chưa đăng ký nhận 403")
    void runningContest_showsProblemsOnlyToContestants() throws Exception {
        moveStartToPast(Duration.ofMinutes(10));

        JsonNode problems = body(send(HttpMethod.GET, PROBLEMS, CONTESTANT, null));
        ResponseEntity<String> outsider = send(HttpMethod.GET, PROBLEMS, OUTSIDER, null);

        assertThat(problems).hasSize(2);
        assertThat(problems.get(0).get("letter").asText()).isEqualTo("A");
        assertThat(problems.get(0).get("title").asText()).isEqualTo("Ghép phòng ban");
        assertThat(problems.get(1).get("letter").asText()).isEqualTo("B");
        assertThat(problems.get(0).has("problemId")).isFalse();
        assertThat(outsider.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(body(outsider).get("code").asText()).isEqualTo("CONTEST_NOT_REGISTERED");
        assertThat(send(HttpMethod.GET, PROBLEMS, ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Contest đã kết thúc thì ai đăng nhập cũng xem được danh sách bài")
    void finishedContest_showsProblemsToEveryone() throws Exception {
        moveStartToPast(Duration.ofDays(1));

        assertThat(body(send(HttpMethod.GET, PROBLEMS, OUTSIDER, null))).hasSize(2);
        assertThat(restTemplate.getForEntity(PROBLEMS, String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void insertProblem(int position, UUID problemId, String title) {
        jdbcTemplate.update(
                "INSERT INTO contest_problems (id, contest_id, problem_id, position, slug, title,"
                        + " difficulty, created_at, updated_at) VALUES (UUID_TO_BIN(UUID()),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(?), ?, ?, ?, 'MEDIUM', NOW(6),"
                        + " NOW(6))",
                contestId,
                problemId.toString(),
                position,
                "bai-" + position,
                title);
    }

    @Test
    @DisplayName("Người thi xem đề từng bài theo chữ cái, chữ cái lạ trả 404, chưa thi thì 403")
    void statement_followsTheSameAccessRules() throws Exception {
        String first = PROBLEMS + "/A";
        assertThat(send(HttpMethod.GET, first, CONTESTANT, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        moveStartToPast(Duration.ofMinutes(10));
        ResponseEntity<String> response = send(HttpMethod.GET, first, CONTESTANT, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode statement = body(response);
        assertThat(statement.get("letter").asText()).isEqualTo("A");
        assertThat(statement.get("title").asText()).isEqualTo("Ghép phòng ban");
        assertThat(statement.get("statementMarkdown").asText()).isEqualTo("Liệt kê phòng ban.");
        assertThat(statement.get("timeLimitMs").asInt()).isEqualTo(2000);
        assertThat(statement.has("problemId")).isFalse();
        assertThat(send(HttpMethod.GET, PROBLEMS + "/Z", CONTESTANT, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.GET, first, OUTSIDER, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void moveStartToPast(Duration ago) {
        jdbcTemplate.update(
                "UPDATE contests SET start_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL ? SECOND),"
                        + " end_at = DATE_ADD(DATE_SUB(UTC_TIMESTAMP(6), INTERVAL ? SECOND),"
                        + " INTERVAL duration_minutes MINUTE)",
                ago.toSeconds(),
                ago.toSeconds());
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private ResponseEntity<String> send(
            HttpMethod method, String path, String token, Object payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                path, method, new HttpEntity<>(payload, headers), String.class);
    }
}
