package com.sqljudge.contest.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class ContestAttemptIntegrationTest extends ContainerSupport {

    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final UUID PROBLEM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ContestAttemptService attemptService;

    private UUID contestId;
    private Instant startAt;

    @BeforeEach
    void createRunningContest() throws Exception {
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
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ADMIN);
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
        jdbcTemplate.update(
                "INSERT INTO contest_problems (id, contest_id, problem_id, position, slug, title,"
                        + " difficulty, created_at, updated_at) VALUES (UUID_TO_BIN(UUID()),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(?), 0, 'bai-a', 'Bài A', 'EASY', NOW(6),"
                        + " NOW(6))",
                contestId.toString(),
                PROBLEM_ID.toString());
    }

    @Test
    @DisplayName("Ghi ô điểm thật và ô công khai, lượt sau đóng băng chỉ hiện là đang chờ")
    void judgedSubmissions_updateActualAndPublicCells() {
        attemptService.record(judged("WA", 10));
        attemptService.record(judged("RE", 12));
        attemptService.record(judged("AC", 65));

        Map<String, Object> cell = cell();
        assertThat(cell)
                .containsEntry("accepted_minute", 65)
                .containsEntry("wrong_attempts", 1)
                .containsEntry("penalty_minutes", 85)
                .containsEntry("public_accepted_minute", null)
                .containsEntry("public_wrong_attempts", 1)
                .containsEntry("pending_attempts", 1);
    }

    @Test
    @DisplayName("Event trùng chỉ tính một lần, chấm lệch thứ tự vẫn ra đúng điểm")
    void duplicateAndOutOfOrderEvents_areScoredOnce() {
        SubmissionJudgedEvent accepted = judged("AC", 30);
        attemptService.record(accepted);
        attemptService.record(judged("WA", 20));
        attemptService.record(accepted);

        assertThat(cell())
                .containsEntry("accepted_minute", 30)
                .containsEntry("wrong_attempts", 1)
                .containsEntry("penalty_minutes", 50);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contest_attempts", Long.class))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("Bỏ qua bài nộp luyện tập, SE và bài không thuộc contest")
    void unrelatedEvents_areIgnored() {
        attemptService.record(
                new SubmissionJudgedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        1,
                        UUID.randomUUID(),
                        USER_ID,
                        PROBLEM_ID,
                        null,
                        "AC",
                        startAt.plus(Duration.ofMinutes(5)),
                        Instant.now()));
        attemptService.record(judged("SE", 6));
        attemptService.record(
                new SubmissionJudgedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        1,
                        UUID.randomUUID(),
                        USER_ID,
                        UUID.randomUUID(),
                        contestId,
                        "AC",
                        startAt.plus(Duration.ofMinutes(7)),
                        Instant.now()));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contest_cells", Long.class))
                .isZero();
    }

    private SubmissionJudgedEvent judged(String verdict, int minute) {
        return new SubmissionJudgedEvent(
                UUID.randomUUID(),
                Instant.now(),
                1,
                UUID.randomUUID(),
                USER_ID,
                PROBLEM_ID,
                contestId,
                verdict,
                startAt.plus(Duration.ofMinutes(minute)),
                Instant.now());
    }

    private Map<String, Object> cell() {
        return jdbcTemplate.queryForMap(
                "SELECT accepted_minute, wrong_attempts, penalty_minutes, public_accepted_minute,"
                        + " public_wrong_attempts, pending_attempts FROM contest_cells"
                        + " WHERE contest_id = UUID_TO_BIN(?) AND user_id = UUID_TO_BIN(?)",
                contestId.toString(),
                USER_ID.toString());
    }
}
