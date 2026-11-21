package com.sqljudge.contest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
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
class ContestApiIntegrationTest extends ContainerSupport {

    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String ADMIN_CONTESTS = "/api/admin/contests";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void clearContests() {
        jdbcTemplate.update("DELETE FROM contests");
    }

    @Test
    @DisplayName("ADMIN tạo contest, nhận 201 kèm Location và giờ kết thúc tính từ thời lượng")
    void create_asAdmin_returnsCreatedContest() throws Exception {
        Instant startAt = Instant.now().plus(Duration.ofDays(2)).truncatedTo(ChronoUnit.SECONDS);

        ResponseEntity<String> response = create(ADMIN, contest("sql-cup-1", startAt));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(response.getHeaders().getLocation())
                .hasPath(ADMIN_CONTESTS + "/" + body.get("id").asText());
        assertThat(body.get("slug").asText()).isEqualTo("sql-cup-1");
        assertThat(body.get("title").asText()).isEqualTo("SQL Cup sql-cup-1");
        assertThat(Instant.parse(body.get("startAt").asText())).isEqualTo(startAt);
        assertThat(Instant.parse(body.get("endAt").asText()))
                .isEqualTo(startAt.plus(Duration.ofMinutes(90)));
        assertThat(body.get("freezeMinutes").asInt()).isEqualTo(30);
        assertThat(body.get("penaltyMinutes").asInt()).isEqualTo(20);
        assertThat(body.get("isRated").asBoolean()).isTrue();
        assertThat(body.get("phase").asText()).isEqualTo("UPCOMING");

        JsonNode detail =
                objectMapper.readTree(
                        send(
                                        HttpMethod.GET,
                                        ADMIN_CONTESTS + "/" + body.get("id").asText(),
                                        ADMIN,
                                        null)
                                .getBody());
        assertThat(detail.get("slug").asText()).isEqualTo("sql-cup-1");
    }

    @Test
    @DisplayName("Không phải ADMIN thì không tạo, không xem được trang quản trị contest")
    void adminEndpoints_asSetter_returnForbidden() {
        Instant startAt = Instant.now().plus(Duration.ofDays(1));

        assertThat(create(SETTER, contest("sql-cup-2", startAt)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.GET, ADMIN_CONTESTS, SETTER, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Dữ liệu sai trả 400 kèm lỗi theo từng trường")
    void create_invalidPayload_returnsFieldErrors() throws Exception {
        Map<String, Object> payload = contest("Sai Slug", Instant.now().plus(Duration.ofDays(1)));
        payload.put("title", "");
        payload.put("durationMinutes", 5);

        ResponseEntity<String> response = create(ADMIN, payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(objectMapper.readTree(response.getBody()).get("errors"))
                .extracting(error -> error.get("field").asText())
                .contains("slug", "title", "durationMinutes");
    }

    @Test
    @DisplayName("Giờ bắt đầu đã qua hoặc đóng băng dài hơn thời lượng thì trả 400")
    void create_pastStartOrLongFreeze_returnsFieldError() throws Exception {
        ResponseEntity<String> past =
                create(ADMIN, contest("qua-khu", Instant.now().minus(Duration.ofMinutes(1))));
        Map<String, Object> longFreeze =
                contest("dong-bang", Instant.now().plus(Duration.ofDays(1)));
        longFreeze.put("freezeMinutes", 90);
        ResponseEntity<String> frozen = create(ADMIN, longFreeze);

        assertThat(past.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(objectMapper.readTree(past.getBody()).at("/errors/0/field").asText())
                .isEqualTo("startAt");
        assertThat(frozen.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(objectMapper.readTree(frozen.getBody()).at("/errors/0/field").asText())
                .isEqualTo("freezeMinutes");
    }

    @Test
    @DisplayName("Slug đã có contest khác dùng thì trả 409")
    void create_duplicateSlug_returnsConflict() throws Exception {
        Instant startAt = Instant.now().plus(Duration.ofDays(1));
        create(ADMIN, contest("trung-slug", startAt));

        ResponseEntity<String> response = create(ADMIN, contest("trung-slug", startAt));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(objectMapper.readTree(response.getBody()).get("code").asText())
                .isEqualTo("SLUG_TAKEN");
    }

    @Test
    @DisplayName("Danh sách công khai lọc theo giai đoạn tính từ giờ server")
    void publicList_filtersByPhase() throws Exception {
        Instant now = Instant.now();
        create(ADMIN, contest("sap-dien-ra", now.plus(Duration.ofDays(3))));
        create(ADMIN, contest("dang-dien-ra", now.plus(Duration.ofDays(1))));
        create(ADMIN, contest("da-ket-thuc", now.plus(Duration.ofDays(1))));
        moveStart("dang-dien-ra", now.minus(Duration.ofMinutes(10)));
        moveStart("da-ket-thuc", now.minus(Duration.ofDays(1)));

        assertThat(slugs(publicList("UPCOMING"))).containsExactly("sap-dien-ra");
        assertThat(slugs(publicList("RUNNING"))).containsExactly("dang-dien-ra");
        assertThat(slugs(publicList("FINISHED"))).containsExactly("da-ket-thuc");
        assertThat(publicList("RUNNING").at("/items/0/phase").asText()).isEqualTo("RUNNING");
        assertThat(publicList("RUNNING").get("totalItems").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Người chưa đăng nhập xem được chi tiết contest theo slug, slug lạ trả 404")
    void publicDetail_bySlug() throws Exception {
        create(ADMIN, contest("sql-cup-3", Instant.now().plus(Duration.ofDays(1))));

        ResponseEntity<String> found =
                restTemplate.getForEntity("/api/contests/sql-cup-3", String.class);
        ResponseEntity<String> missing =
                restTemplate.getForEntity("/api/contests/khong-co", String.class);

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(found.getBody()).get("description").asText())
                .isEqualTo("Thi SQL 90 phút");
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(objectMapper.readTree(missing.getBody()).get("code").asText())
                .isEqualTo("CONTEST_NOT_FOUND");
    }

    @Test
    @DisplayName("ADMIN xem mọi contest, giờ bắt đầu muộn nhất lên đầu")
    void adminList_ordersByStartDescending() throws Exception {
        Instant now = Instant.now();
        create(ADMIN, contest("som", now.plus(Duration.ofDays(1))));
        create(ADMIN, contest("muon", now.plus(Duration.ofDays(5))));

        JsonNode page =
                objectMapper.readTree(send(HttpMethod.GET, ADMIN_CONTESTS, ADMIN, null).getBody());

        assertThat(slugs(page)).containsExactly("muon", "som");
    }

    private JsonNode publicList(String phase) throws Exception {
        return objectMapper.readTree(
                restTemplate.getForObject("/api/contests?phase=" + phase, String.class));
    }

    private void moveStart(String slug, Instant startAt) {
        jdbcTemplate.update(
                "UPDATE contests SET start_at = ?, end_at = DATE_ADD(?, INTERVAL duration_minutes"
                        + " MINUTE) WHERE slug = ?",
                LocalDateTime.ofInstant(startAt, ZoneOffset.UTC),
                LocalDateTime.ofInstant(startAt, ZoneOffset.UTC),
                slug);
    }

    private static Iterable<String> slugs(JsonNode page) {
        return () ->
                StreamSupport.stream(page.get("items").spliterator(), false)
                        .map(item -> item.get("slug").asText())
                        .iterator();
    }

    private static Map<String, Object> contest(String slug, Instant startAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("slug", slug);
        payload.put("title", "SQL Cup " + slug);
        payload.put("description", "Thi SQL 90 phút");
        payload.put("startAt", startAt.toString());
        payload.put("durationMinutes", 90);
        payload.put("freezeMinutes", 30);
        payload.put("penaltyMinutes", 20);
        payload.put("isRated", true);
        return payload;
    }

    private ResponseEntity<String> create(String token, Map<String, Object> payload) {
        return send(HttpMethod.POST, ADMIN_CONTESTS, token, payload);
    }

    private ResponseEntity<String> send(
            HttpMethod method, String path, String token, Object payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                path, method, new HttpEntity<>(payload, headers), String.class);
    }
}
