package com.sqljudge.contest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
class ContestProblemsApiIntegrationTest extends ContainerSupport {

    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final UUID JOIN_PROBLEM = UUID.randomUUID();
    private static final UUID WINDOW_PROBLEM = UUID.randomUUID();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private String contestId;

    @DynamicPropertySource
    static void problemService(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.problem-service-url", ProblemServiceStub::url);
    }

    @BeforeEach
    void createContest() throws Exception {
        jdbcTemplate.update("DELETE FROM contest_rating_changes");
        jdbcTemplate.update("DELETE FROM contest_standings");
        jdbcTemplate.update("DELETE FROM contest_cells");
        jdbcTemplate.update("DELETE FROM contest_attempts");
        jdbcTemplate.update("DELETE FROM contest_registrations");
        jdbcTemplate.update("DELETE FROM contest_problems");
        jdbcTemplate.update("DELETE FROM contests");
        ProblemServiceStub.reset();
        ProblemServiceStub.ready(JOIN_PROBLEM, "join-phong-ban", "Ghép phòng ban");
        ProblemServiceStub.ready(WINDOW_PROBLEM, "xep-hang-luong", "Xếp hạng lương");
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
                body(send(HttpMethod.POST, "/api/admin/contests", ADMIN, contest))
                        .get("id")
                        .asText();
    }

    @Test
    @DisplayName(
            "ADMIN gắn bài theo thứ tự, mỗi bài nhận chữ cái A, B và tên lấy từ problem-service")
    void replaceProblems_storesOrderedLetters() throws Exception {
        ResponseEntity<String> response = replace(ADMIN, List.of(WINDOW_PROBLEM, JOIN_PROBLEM));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode problems = body(send(HttpMethod.GET, adminContest(), ADMIN, null)).get("problems");
        assertThat(problems).hasSize(2);
        assertThat(problems.get(0).get("letter").asText()).isEqualTo("A");
        assertThat(problems.get(0).get("problemId").asText()).isEqualTo(WINDOW_PROBLEM.toString());
        assertThat(problems.get(0).get("title").asText()).isEqualTo("Xếp hạng lương");
        assertThat(problems.get(1).get("letter").asText()).isEqualTo("B");
        assertThat(problems.get(1).get("slug").asText()).isEqualTo("join-phong-ban");
        assertThat(ProblemServiceStub.receivedToken()).isEqualTo("test-internal-token");

        replace(ADMIN, List.of(JOIN_PROBLEM));
        JsonNode replaced = body(send(HttpMethod.GET, adminContest(), ADMIN, null)).get("problems");
        assertThat(replaced).hasSize(1);
        assertThat(replaced.get(0).get("problemId").asText()).isEqualTo(JOIN_PROBLEM.toString());
    }

    @Test
    @DisplayName("Bài chưa duyệt cho contest thì trả 422 và không lưu gì")
    void replaceProblems_notReadyProblem_isRejected() throws Exception {
        ResponseEntity<String> response = replace(ADMIN, List.of(JOIN_PROBLEM, UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(body(response).get("code").asText()).isEqualTo("CONTEST_PROBLEM_NOT_READY");
        assertThat(problemCount()).isZero();
    }

    @Test
    @DisplayName("Danh sách trùng bài thì trả 400")
    void replaceProblems_duplicateIds_returnsBadRequest() {
        assertThat(replace(ADMIN, List.of(JOIN_PROBLEM, JOIN_PROBLEM)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Contest đã bắt đầu thì không đổi bài được nữa")
    void replaceProblems_startedContest_returnsConflict() throws Exception {
        jdbcTemplate.update(
                "UPDATE contests SET start_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 MINUTE)");

        ResponseEntity<String> response = replace(ADMIN, List.of(JOIN_PROBLEM));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(body(response).get("code").asText()).isEqualTo("CONTEST_ALREADY_STARTED");
    }

    @Test
    @DisplayName("Không gọi được problem-service thì trả 503")
    void replaceProblems_problemServiceDown_returnsServiceUnavailable() throws Exception {
        ProblemServiceStub.failWith(500);

        ResponseEntity<String> response = replace(ADMIN, List.of(JOIN_PROBLEM));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(body(response).get("code").asText()).isEqualTo("PROBLEM_SERVICE_UNAVAILABLE");
    }

    @Test
    @DisplayName("Chỉ ADMIN gắn bài được, trang công khai không lộ danh sách bài")
    void problems_hiddenFromNonAdmins() throws Exception {
        replace(ADMIN, List.of(JOIN_PROBLEM));

        assertThat(replace(SETTER, List.of(WINDOW_PROBLEM)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        String publicDetail = restTemplate.getForObject("/api/contests/sql-cup", String.class);
        assertThat(publicDetail).doesNotContain("problems").doesNotContain("join-phong-ban");
    }

    private ResponseEntity<String> replace(String token, List<UUID> problemIds) {
        return send(
                HttpMethod.PUT,
                adminContest() + "/problems",
                token,
                Map.of("problemIds", problemIds));
    }

    private String adminContest() {
        return "/api/admin/contests/" + contestId;
    }

    private long problemCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contest_problems", Long.class);
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
