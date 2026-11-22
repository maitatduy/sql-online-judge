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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class ContestEntryInternalApiIntegrationTest extends ContainerSupport {

    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String INTERNAL_TOKEN = "test-internal-token";
    private static final UUID CONTESTANT_ID = UUID.randomUUID();
    private static final UUID PROBLEM_ID = UUID.randomUUID();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private String contestId;

    @BeforeEach
    void createRunningContest() throws Exception {
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
                objectMapper
                        .readTree(
                                restTemplate.postForObject(
                                        "/api/admin/contests",
                                        new HttpEntity<>(contest, headers),
                                        String.class))
                        .get("id")
                        .asText();
        jdbcTemplate.update(
                "INSERT INTO contest_problems (id, contest_id, problem_id, position, slug, title,"
                        + " difficulty, created_at, updated_at) VALUES (UUID_TO_BIN(UUID()),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(?), 0, 'bai-a', 'Bài A', 'EASY', NOW(6),"
                        + " NOW(6))",
                contestId,
                PROBLEM_ID.toString());
        jdbcTemplate.update(
                "INSERT INTO contest_registrations (id, contest_id, user_id, username, created_at,"
                        + " updated_at) VALUES (UUID_TO_BIN(UUID()), UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), 'thi.sinh', NOW(6), NOW(6))",
                contestId,
                CONTESTANT_ID.toString());
    }

    @Test
    @DisplayName("Người đã đăng ký nộp được khi contest đang diễn ra, nhận id contest và id bài")
    void entry_runningAndRegistered_returnsIds() throws Exception {
        moveStartToPast(Duration.ofMinutes(10));

        ResponseEntity<String> response = entry("A", CONTESTANT_ID, INTERNAL_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("contestId").asText()).isEqualTo(contestId);
        assertThat(body.get("problemId").asText()).isEqualTo(PROBLEM_ID.toString());
        assertThat(body.get("letter").asText()).isEqualTo("A");
    }

    @Test
    @DisplayName("Chưa bắt đầu, chưa đăng ký, đã kết thúc hoặc chữ cái lạ đều bị từ chối")
    void entry_rejectsIneligibleRequests() throws Exception {
        assertThat(code(entry("A", CONTESTANT_ID, INTERNAL_TOKEN)))
                .isEqualTo("CONTEST_NOT_STARTED");

        moveStartToPast(Duration.ofMinutes(10));
        ResponseEntity<String> outsider = entry("A", UUID.randomUUID(), INTERNAL_TOKEN);
        assertThat(outsider.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(code(outsider)).isEqualTo("CONTEST_NOT_REGISTERED");
        assertThat(entry("Z", CONTESTANT_ID, INTERNAL_TOKEN).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        moveStartToPast(Duration.ofDays(1));
        ResponseEntity<String> finished = entry("A", CONTESTANT_ID, INTERNAL_TOKEN);
        assertThat(finished.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(code(finished)).isEqualTo("CONTEST_ALREADY_FINISHED");
    }

    @Test
    @DisplayName("Thiếu token nội bộ trả 401")
    void entry_withoutToken_isUnauthorized() {
        assertThat(entry("A", CONTESTANT_ID, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<String> entry(String letter, UUID userId, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set("X-Internal-Token", token);
        }
        return restTemplate.exchange(
                "/internal/contests/sql-cup/problems/" + letter + "/entry?userId=" + userId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }

    private String code(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody()).get("code").asText();
    }

    private void moveStartToPast(Duration ago) {
        jdbcTemplate.update(
                "UPDATE contests SET start_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL ? SECOND),"
                        + " end_at = DATE_ADD(DATE_SUB(UTC_TIMESTAMP(6), INTERVAL ? SECOND),"
                        + " INTERVAL duration_minutes MINUTE)",
                ago.toSeconds(),
                ago.toSeconds());
    }
}
