package com.sqljudge.contest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.contest.event.SubmissionJudgedEvent;
import com.sqljudge.contest.service.ContestAttemptService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class ContestStandingsApiIntegrationTest extends ContainerSupport {

    private static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.4-alpine").withExposedPorts(REDIS_PORT);
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final UUID PROBLEM_A = UUID.randomUUID();
    private static final UUID PROBLEM_B = UUID.randomUUID();
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CAROL = UUID.randomUUID();
    private static final UUID ERIN = UUID.randomUUID();
    private static final String STANDINGS = "/api/contests/sql-cup/standings";

    static {
        REDIS.start();
    }

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ContestAttemptService attemptService;
    @Autowired private StringRedisTemplate redisTemplate;

    private UUID contestId;
    private Instant startAt;

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
        registry.add("spring.data.redis.timeout", () -> "1s");
    }

    @BeforeEach
    void createRunningContest() throws Exception {
        jdbcTemplate.update("DELETE FROM contest_rating_changes");
        jdbcTemplate.update("DELETE FROM contest_standings");
        jdbcTemplate.update("DELETE FROM contest_cells");
        jdbcTemplate.update("DELETE FROM contest_attempts");
        jdbcTemplate.update("DELETE FROM contest_registrations");
        jdbcTemplate.update("DELETE FROM contest_problems");
        jdbcTemplate.update("DELETE FROM contests");
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ADMIN);
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
                UUID.fromString(
                        objectMapper
                                .readTree(
                                        restTemplate.postForObject(
                                                "/api/admin/contests",
                                                new HttpEntity<>(contest, headers),
                                                String.class))
                                .get("id")
                                .asText());
        startAt = Instant.now().minus(Duration.ofMinutes(80)).truncatedTo(ChronoUnit.SECONDS);
        jdbcTemplate.update(
                "UPDATE contests SET start_at = ?, end_at = ? WHERE id = UUID_TO_BIN(?)",
                LocalDateTime.ofInstant(startAt, ZoneOffset.UTC),
                LocalDateTime.ofInstant(startAt.plus(Duration.ofMinutes(90)), ZoneOffset.UTC),
                contestId.toString());
        insertProblem(0, PROBLEM_A);
        insertProblem(1, PROBLEM_B);
        register(ALICE, "alice");
        register(BOB, "bob");
        register(CAROL, "carol");
        register(ERIN, "erin");
    }

    @Test
    @DisplayName("Xếp theo số bài rồi tổng phạt, bằng điểm thì cùng hạng, kèm ô từng bài")
    void standings_rankBySolvedThenPenalty() throws Exception {
        playContest();

        JsonNode standings = body(restTemplate.getForEntity(STANDINGS, String.class));

        assertThat(standings.get("isFrozen").asBoolean()).isTrue();
        assertThat(standings.get("problems").toString()).isEqualTo("[\"A\",\"B\"]");
        assertThat(standings.get("totalItems").asInt()).isEqualTo(4);
        JsonNode items = standings.get("items");
        assertThat(items.get(0).get("rank").asInt()).isEqualTo(1);
        assertThat(items.get(1).get("rank").asInt()).isEqualTo(1);
        assertThat(items.get(2).get("username").asText()).isEqualTo("alice");
        assertThat(items.get(2).get("rank").asInt()).isEqualTo(3);
        assertThat(items.get(2).get("solved").asInt()).isEqualTo(2);
        assertThat(items.get(2).get("penaltyMinutes").asInt()).isEqualTo(70);
        JsonNode aliceB = items.get(2).get("cells").get(1);
        assertThat(aliceB.get("letter").asText()).isEqualTo("B");
        assertThat(aliceB.get("acceptedMinute").asInt()).isEqualTo(40);
        assertThat(aliceB.get("wrongAttempts").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sau mốc đóng băng bảng công khai không đổi, lượt mới hiện là đang chờ")
    void standings_hideFrozenAttemptsFromThePublic() throws Exception {
        playContest();

        JsonNode carol =
                body(restTemplate.getForEntity(STANDINGS, String.class)).get("items").get(3);

        assertThat(carol.get("username").asText()).isEqualTo("carol");
        assertThat(carol.get("rank").asInt()).isEqualTo(4);
        assertThat(carol.get("solved").asInt()).isZero();
        JsonNode carolA = carol.get("cells").get(0);
        assertThat(carolA.get("acceptedMinute").isNull()).isTrue();
        assertThat(carolA.get("wrongAttempts").asInt()).isEqualTo(1);
        assertThat(carolA.get("pendingAttempts").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Người thi luôn thấy kết quả thật của mình, kể cả khi đã đóng băng")
    void myStanding_showsActualResultDuringFreeze() throws Exception {
        playContest();

        ResponseEntity<String> response =
                restTemplate.exchange(
                        STANDINGS + "/me",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(TestJwts.token(CAROL, "LEARNER"))),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode me = body(response);
        assertThat(me.get("solved").asInt()).isEqualTo(1);
        assertThat(me.get("penaltyMinutes").asInt()).isEqualTo(85);
        assertThat(me.get("cells").get(0).get("acceptedMinute").asInt()).isEqualTo(65);
    }

    @Test
    @DisplayName("Mất dữ liệu Redis thì bảng được dựng lại từ MySQL")
    void standings_rebuildFromDatabaseWhenRedisIsEmpty() throws Exception {
        playContest();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        JsonNode standings = body(restTemplate.getForEntity(STANDINGS, String.class));

        assertThat(standings.get("totalItems").asInt()).isEqualTo(4);
        assertThat(standings.get("items").get(2).get("username").asText()).isEqualTo("alice");
        assertThat(redisTemplate.getExpire("sqljudge:contest:standings:" + contestId)).isPositive();
    }

    @Test
    @DisplayName("Redis không phản hồi thì bảng vẫn được tính từ MySQL, không trả lỗi")
    void standings_fallBackToDatabaseWhenRedisIsDown() throws Exception {
        playContest();
        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(STANDINGS, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode standings = body(response);
            assertThat(standings.get("totalItems").asInt()).isEqualTo(4);
            JsonNode items = standings.get("items");
            assertThat(items.get(0).get("rank").asInt()).isEqualTo(1);
            assertThat(items.get(1).get("rank").asInt()).isEqualTo(1);
            assertThat(items.get(2).get("username").asText()).isEqualTo("alice");
            assertThat(items.get(2).get("rank").asInt()).isEqualTo(3);
            assertThat(items.get(2).get("penaltyMinutes").asInt()).isEqualTo(70);
        } finally {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }
    }

    private void playContest() {
        judged(ALICE, PROBLEM_A, "AC", 10);
        judged(ALICE, PROBLEM_B, "WA", 20);
        judged(ALICE, PROBLEM_B, "AC", 40);
        judged(BOB, PROBLEM_A, "AC", 5);
        judged(BOB, PROBLEM_B, "AC", 15);
        judged(ERIN, PROBLEM_A, "AC", 5);
        judged(ERIN, PROBLEM_B, "AC", 15);
        judged(CAROL, PROBLEM_A, "WA", 30);
        judged(CAROL, PROBLEM_A, "AC", 65);
    }

    private void judged(UUID userId, UUID problemId, String verdict, int minute) {
        attemptService.record(
                new SubmissionJudgedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        1,
                        UUID.randomUUID(),
                        userId,
                        problemId,
                        contestId,
                        verdict,
                        startAt.plus(Duration.ofMinutes(minute)),
                        Instant.now()));
    }

    private void insertProblem(int position, UUID problemId) {
        jdbcTemplate.update(
                "INSERT INTO contest_problems (id, contest_id, problem_id, position, slug, title,"
                        + " difficulty, created_at, updated_at) VALUES (UUID_TO_BIN(UUID()),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(?), ?, ?, 'Bài', 'EASY', NOW(6), NOW(6))",
                contestId.toString(),
                problemId.toString(),
                position,
                "bai-" + position);
    }

    private void register(UUID userId, String username) {
        jdbcTemplate.update(
                "INSERT INTO contest_registrations (id, contest_id, user_id, username, created_at,"
                        + " updated_at) VALUES (UUID_TO_BIN(UUID()), UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), ?, NOW(6), NOW(6))",
                contestId.toString(),
                userId.toString(),
                username);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }
}
