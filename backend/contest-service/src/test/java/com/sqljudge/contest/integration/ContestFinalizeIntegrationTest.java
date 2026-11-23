package com.sqljudge.contest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.contest.event.SubmissionJudgedEvent;
import com.sqljudge.contest.service.ContestAttemptService;
import com.sqljudge.contest.service.ContestFinalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class ContestFinalizeIntegrationTest extends ContainerSupport {

    private static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.4-alpine").withExposedPorts(REDIS_PORT);
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final UUID PROBLEM_A = UUID.randomUUID();
    private static final UUID PROBLEM_B = UUID.randomUUID();
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CAROL = UUID.randomUUID();

    static {
        REDIS.start();
    }

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ContestAttemptService attemptService;
    @Autowired private ContestFinalizer finalizer;
    @Autowired private StringRedisTemplate redisTemplate;

    private UUID contestId;
    private Instant startAt;

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
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
        moveStart(startAt);
        insertProblem(0, PROBLEM_A);
        insertProblem(1, PROBLEM_B);
        register(ALICE, "alice");
        register(BOB, "bob");
        register(CAROL, "carol");
        judged(ALICE, PROBLEM_A, "AC", 10);
        judged(BOB, PROBLEM_A, "AC", 5);
        judged(BOB, PROBLEM_B, "AC", 15);
        judged(CAROL, PROBLEM_A, "WA", 30);
        judged(CAROL, PROBLEM_A, "AC", 65);
        judged(CAROL, PROBLEM_B, "AC", 70);
    }

    @Test
    @DisplayName("Chưa hết giờ chờ chấm thì chưa chốt, bảng vẫn đóng băng")
    void runningContest_isNotFinalized() throws Exception {
        finalizer.finalizeDue();

        assertThat(countStandings()).isZero();
        assertThat(standings().get("isFrozen").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Hết giờ thì ghi bảng cuối vào MySQL, bỏ đóng băng và chỉ chốt một lần")
    void finishedContest_isFinalizedOnce() throws Exception {
        assertThat(standings().get("items").get(0).get("username").asText()).isEqualTo("bob");
        moveStart(startAt.minus(Duration.ofMinutes(20)));

        finalizer.finalizeDue();
        finalizer.finalizeDue();

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(
                        "SELECT username, rank_position, solved, penalty_minutes"
                                + " FROM contest_standings ORDER BY rank_position, username");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0)).containsEntry("username", "bob").containsEntry("rank_position", 1);
        assertThat(rows.get(1))
                .containsEntry("username", "carol")
                .containsEntry("rank_position", 2)
                .containsEntry("solved", 2);
        assertThat(rows.get(2))
                .containsEntry("username", "alice")
                .containsEntry("rank_position", 3);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT finalized_at IS NOT NULL FROM contests", Boolean.class))
                .isTrue();
        JsonNode standings = standings();
        assertThat(standings.get("isFrozen").asBoolean()).isFalse();
        assertThat(standings.get("items").get(1).get("username").asText()).isEqualTo("carol");
        assertThat(standings.get("items").get(1).get("solved").asInt()).isEqualTo(2);
    }

    private JsonNode standings() throws Exception {
        return objectMapper.readTree(
                restTemplate.getForObject("/api/contests/sql-cup/standings", String.class));
    }

    private long countStandings() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contest_standings", Long.class);
    }

    private void moveStart(Instant newStart) {
        jdbcTemplate.update(
                "UPDATE contests SET start_at = ?, end_at = ? WHERE id = UUID_TO_BIN(?)",
                LocalDateTime.ofInstant(newStart, ZoneOffset.UTC),
                LocalDateTime.ofInstant(newStart.plus(Duration.ofMinutes(90)), ZoneOffset.UTC),
                contestId.toString());
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
}
