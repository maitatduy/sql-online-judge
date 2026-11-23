package com.sqljudge.contest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.contest.service.ContestLifecycleService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class ContestLifecycleIntegrationTest extends ContainerSupport {

    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String STARTED = "sqljudge.contest.started";
    private static final String FINISHED = "sqljudge.contest.finished";
    private static final UUID PROBLEM_ID = UUID.randomUUID();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ContestLifecycleService lifecycleService;

    @BeforeEach
    void clear() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM contest_standings");
        jdbcTemplate.update("DELETE FROM contest_cells");
        jdbcTemplate.update("DELETE FROM contest_attempts");
        jdbcTemplate.update("DELETE FROM contest_registrations");
        jdbcTemplate.update("DELETE FROM contest_problems");
        jdbcTemplate.update("DELETE FROM contests");
    }

    @Test
    @DisplayName(
            "Contest chưa tới giờ thì chưa phát gì, tới giờ thì phát contest.started đúng một lần")
    void startedEvent_isAnnouncedOnce() throws Exception {
        String contestId = createContest("sql-cup", true);

        lifecycleService.announceDue();
        assertThat(events(STARTED)).isEmpty();

        moveStartToPast("sql-cup", Duration.ofMinutes(5));
        lifecycleService.announceDue();
        lifecycleService.announceDue();

        List<Map<String, Object>> started = events(STARTED);
        assertThat(started).hasSize(1);
        assertThat(started.get(0).get("message_key")).isEqualTo(contestId);
        JsonNode payload = objectMapper.readTree((String) started.get(0).get("payload"));
        assertThat(payload.get("contestId").asText()).isEqualTo(contestId);
        assertThat(payload.get("isRated").asBoolean()).isTrue();
        assertThat(payload.get("eventId").asText()).isNotBlank();
        assertThat(events(FINISHED)).isEmpty();
    }

    @Test
    @DisplayName("Contest đã kết thúc thì phát cả started lẫn finished, finished kèm danh sách bài")
    void finishedEvent_carriesProblemIds() throws Exception {
        String contestId = createContest("sql-cup-xong", false);
        jdbcTemplate.update(
                "INSERT INTO contest_problems (id, contest_id, problem_id, position, slug, title,"
                        + " difficulty, created_at, updated_at) VALUES (UUID_TO_BIN(UUID()),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(?), 0, 'bai-a', 'Bài A', 'EASY', NOW(6),"
                        + " NOW(6))",
                contestId,
                PROBLEM_ID.toString());
        moveStartToPast("sql-cup-xong", Duration.ofDays(1));

        lifecycleService.announceDue();
        lifecycleService.announceDue();

        assertThat(events(STARTED)).hasSize(1);
        List<Map<String, Object>> finished = events(FINISHED);
        assertThat(finished).hasSize(1);
        JsonNode payload = objectMapper.readTree((String) finished.get(0).get("payload"));
        assertThat(payload.get("contestId").asText()).isEqualTo(contestId);
        assertThat(payload.get("problemIds").get(0).asText()).isEqualTo(PROBLEM_ID.toString());
    }

    private List<Map<String, Object>> events(String topic) {
        return jdbcTemplate.queryForList(
                "SELECT message_key, payload FROM outbox_events WHERE topic = ?", topic);
    }

    private String createContest(String slug, boolean isRated) throws Exception {
        Map<String, Object> contest =
                Map.of(
                        "slug",
                        slug,
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
                        isRated);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ADMIN);
        String body =
                restTemplate.postForObject(
                        "/api/admin/contests", new HttpEntity<>(contest, headers), String.class);
        return objectMapper.readTree(body).get("id").asText();
    }

    private void moveStartToPast(String slug, Duration ago) {
        jdbcTemplate.update(
                "UPDATE contests SET start_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL ? SECOND),"
                        + " end_at = DATE_ADD(DATE_SUB(UTC_TIMESTAMP(6), INTERVAL ? SECOND),"
                        + " INTERVAL duration_minutes MINUTE) WHERE slug = ?",
                ago.toSeconds(),
                ago.toSeconds(),
                slug);
    }
}
