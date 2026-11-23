package com.sqljudge.contest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.contest.service.ContestRatingService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class ContestRatingIntegrationTest extends ContainerSupport {

    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String RATING_UPDATED = "sqljudge.contest.rating-updated";
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ContestRatingService ratingService;

    @BeforeEach
    void clear() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM contest_rating_changes");
        jdbcTemplate.update("DELETE FROM user_ratings");
        jdbcTemplate.update("DELETE FROM contest_standings");
        jdbcTemplate.update("DELETE FROM contest_cells");
        jdbcTemplate.update("DELETE FROM contest_attempts");
        jdbcTemplate.update("DELETE FROM contest_registrations");
        jdbcTemplate.update("DELETE FROM contest_problems");
        jdbcTemplate.update("DELETE FROM contests");
    }

    @Test
    @DisplayName("Contest tính rating đã chốt thì áp Elo một lần, lưu thay đổi và phát event")
    void finalizedRatedContest_appliesRatingOnce() throws Exception {
        String contestId = finalizedContest("sql-cup", true);
        jdbcTemplate.update(
                "INSERT INTO user_ratings (id, user_id, rating, contests_played, version,"
                        + " created_at, updated_at) VALUES (UUID_TO_BIN(UUID()), UUID_TO_BIN(?),"
                        + " 1600, 3, 0, NOW(6), NOW(6))",
                BOB.toString());

        ratingService.applyDue();
        ratingService.applyDue();

        Map<String, Object> alice = rating(ALICE);
        Map<String, Object> bob = rating(BOB);
        assertThat(alice.get("rating")).isEqualTo(1500 + 20);
        assertThat(alice.get("contests_played")).isEqualTo(1);
        assertThat(bob.get("rating")).isEqualTo(1600 - 20);
        assertThat(bob.get("contests_played")).isEqualTo(4);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM contest_rating_changes", Long.class))
                .isEqualTo(2);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT rating_applied_at IS NOT NULL FROM contests",
                                Boolean.class))
                .isTrue();
        JsonNode event =
                objectMapper.readTree(
                        jdbcTemplate.queryForObject(
                                "SELECT payload FROM outbox_events WHERE topic = ?",
                                String.class,
                                RATING_UPDATED));
        assertThat(event.get("contestId").asText()).isEqualTo(contestId);
        assertThat(event.get("changes")).hasSize(2);

        JsonNode changes =
                objectMapper.readTree(
                        restTemplate.getForObject(
                                "/api/contests/sql-cup/rating-changes", String.class));
        assertThat(changes.get(0).get("username").asText()).isEqualTo("alice");
        assertThat(changes.get(0).get("rank").asInt()).isEqualTo(1);
        assertThat(changes.get(0).get("oldRating").asInt()).isEqualTo(1500);
        assertThat(changes.get(0).get("newRating").asInt()).isEqualTo(1520);
        assertThat(changes.get(0).get("delta").asInt()).isEqualTo(20);
    }

    @Test
    @DisplayName("Contest không tính rating thì không đổi rating của ai")
    void unratedContest_isSkipped() throws Exception {
        finalizedContest("vui-ve", false);

        ratingService.applyDue();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_ratings", Long.class))
                .isZero();
    }

    private Map<String, Object> rating(UUID userId) {
        return jdbcTemplate.queryForMap(
                "SELECT rating, contests_played FROM user_ratings WHERE user_id = UUID_TO_BIN(?)",
                userId.toString());
    }

    private String finalizedContest(String slug, boolean isRated) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ADMIN);
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
        String contestId =
                objectMapper
                        .readTree(
                                restTemplate.postForObject(
                                        "/api/admin/contests",
                                        new HttpEntity<>(contest, headers),
                                        String.class))
                        .get("id")
                        .asText();
        jdbcTemplate.update(
                "UPDATE contests SET start_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 HOUR),"
                        + " end_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 90 MINUTE),"
                        + " finalized_at = UTC_TIMESTAMP(6) WHERE id = UUID_TO_BIN(?)",
                contestId);
        standing(contestId, ALICE, "alice", 1);
        standing(contestId, BOB, "bob", 2);
        return contestId;
    }

    private void standing(String contestId, UUID userId, String username, int rank) {
        jdbcTemplate.update(
                "INSERT INTO contest_standings (id, contest_id, user_id, username, rank_position,"
                        + " solved, penalty_minutes, created_at, updated_at) VALUES"
                        + " (UUID_TO_BIN(UUID()), UUID_TO_BIN(?), UUID_TO_BIN(?), ?, ?, 1, 10,"
                        + " NOW(6), NOW(6))",
                contestId,
                userId.toString(),
                username,
                rank);
    }
}
