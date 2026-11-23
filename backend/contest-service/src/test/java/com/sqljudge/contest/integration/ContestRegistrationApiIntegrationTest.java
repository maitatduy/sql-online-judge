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
class ContestRegistrationApiIntegrationTest extends ContainerSupport {

    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final UUID LEARNER_ID = UUID.randomUUID();
    private static final UUID BANNED_ID = UUID.randomUUID();
    private static final String LEARNER = TestJwts.token(LEARNER_ID, "LEARNER");
    private static final String BANNED = TestJwts.token(BANNED_ID, "LEARNER");
    private static final String REGISTRATION = "/api/contests/sql-cup/registration";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void authService(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.auth-service-url", AuthServiceStub::url);
    }

    @BeforeEach
    void createContest() {
        jdbcTemplate.update("DELETE FROM contest_standings");
        jdbcTemplate.update("DELETE FROM contest_cells");
        jdbcTemplate.update("DELETE FROM contest_attempts");
        jdbcTemplate.update("DELETE FROM contest_registrations");
        jdbcTemplate.update("DELETE FROM contest_problems");
        jdbcTemplate.update("DELETE FROM contests");
        AuthServiceStub.reset();
        AuthServiceStub.user(LEARNER_ID, "hoc.vien", false);
        AuthServiceStub.user(BANNED_ID, "bi.cam", true);
        createContest(true);
    }

    @Test
    @DisplayName("Người học đăng ký thì thấy đã đăng ký, đăng ký lại vẫn chỉ tính một lần")
    void register_isIdempotent() throws Exception {
        ResponseEntity<String> first = send(HttpMethod.POST, REGISTRATION, LEARNER);
        ResponseEntity<String> again = send(HttpMethod.POST, REGISTRATION, LEARNER);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(body(first).get("isRegistered").asBoolean()).isTrue();
        assertThat(body(first).get("registeredAt").isNull()).isFalse();
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(
                        body(send(HttpMethod.GET, REGISTRATION, LEARNER))
                                .get("isRegistered")
                                .asBoolean())
                .isTrue();
        assertThat(publicDetail().get("registrantCount").asInt()).isEqualTo(1);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT username FROM contest_registrations", String.class))
                .isEqualTo("hoc.vien");
    }

    @Test
    @DisplayName("Huỷ đăng ký trước giờ bắt đầu thì không còn trong danh sách")
    void unregister_beforeStart_removesRegistration() throws Exception {
        send(HttpMethod.POST, REGISTRATION, LEARNER);

        ResponseEntity<String> response = send(HttpMethod.DELETE, REGISTRATION, LEARNER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        JsonNode status = body(send(HttpMethod.GET, REGISTRATION, LEARNER));
        assertThat(status.get("isRegistered").asBoolean()).isFalse();
        assertThat(status.get("registeredAt").isNull()).isTrue();
        assertThat(publicDetail().get("registrantCount").asInt()).isZero();
    }

    @Test
    @DisplayName("Contest đang diễn ra vẫn đăng ký muộn được nhưng không huỷ được")
    void runningContest_allowsLateRegistrationOnly() throws Exception {
        moveStartToPast(Duration.ofMinutes(10));

        assertThat(send(HttpMethod.POST, REGISTRATION, LEARNER).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        ResponseEntity<String> unregister = send(HttpMethod.DELETE, REGISTRATION, LEARNER);
        assertThat(unregister.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(body(unregister).get("code").asText()).isEqualTo("CONTEST_ALREADY_STARTED");
    }

    @Test
    @DisplayName("Contest đã kết thúc thì không đăng ký được nữa")
    void finishedContest_rejectsRegistration() throws Exception {
        moveStartToPast(Duration.ofDays(1));

        ResponseEntity<String> response = send(HttpMethod.POST, REGISTRATION, LEARNER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(body(response).get("code").asText()).isEqualTo("CONTEST_ALREADY_FINISHED");
    }

    @Test
    @DisplayName(
            "Tài khoản bị cấm thi contest tính rating nhận 403, contest không tính rating vẫn vào"
                    + " được")
    void bannedUser_isRejectedOnlyFromRatedContests() throws Exception {
        ResponseEntity<String> rated = send(HttpMethod.POST, REGISTRATION, BANNED);

        assertThat(rated.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(body(rated).get("code").asText()).isEqualTo("RATED_CONTEST_BANNED");

        jdbcTemplate.update("DELETE FROM contests");
        createContest(false);
        assertThat(send(HttpMethod.POST, REGISTRATION, BANNED).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName(
            "Không gọi được auth-service thì trả 503, chưa đăng nhập thì 401, contest lạ thì 404")
    void registration_failureCases() throws Exception {
        AuthServiceStub.failWith(500);
        ResponseEntity<String> unavailable = send(HttpMethod.POST, REGISTRATION, LEARNER);
        assertThat(unavailable.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(body(unavailable).get("code").asText()).isEqualTo("AUTH_SERVICE_UNAVAILABLE");

        assertThat(restTemplate.postForEntity(REGISTRATION, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(
                        send(HttpMethod.POST, "/api/contests/khong-co/registration", LEARNER)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void createContest(boolean isRated) {
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
                        isRated);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ADMIN);
        restTemplate.postForEntity(
                "/api/admin/contests", new HttpEntity<>(contest, headers), String.class);
    }

    private void moveStartToPast(Duration ago) {
        jdbcTemplate.update(
                "UPDATE contests SET start_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL ? SECOND),"
                        + " end_at = DATE_ADD(DATE_SUB(UTC_TIMESTAMP(6), INTERVAL ? SECOND),"
                        + " INTERVAL duration_minutes MINUTE)",
                ago.toSeconds(),
                ago.toSeconds());
    }

    private JsonNode publicDetail() throws Exception {
        return objectMapper.readTree(
                restTemplate.getForObject("/api/contests/sql-cup", String.class));
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, method, new HttpEntity<>(headers), String.class);
    }
}
