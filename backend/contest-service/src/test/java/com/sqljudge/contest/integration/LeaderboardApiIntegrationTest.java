package com.sqljudge.contest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestRatingChange;
import com.sqljudge.contest.entity.ContestSchedule;
import com.sqljudge.contest.repository.ContestRatingChangeRepository;
import com.sqljudge.contest.repository.ContestRepository;
import com.sqljudge.contest.scoring.EloRating.Change;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class LeaderboardApiIntegrationTest extends ContainerSupport {

    private static final UUID TOP = UUID.randomUUID();
    private static final UUID SECOND = UUID.randomUUID();
    private static final UUID THIRD = UUID.randomUUID();
    private static final UUID NEWCOMER = UUID.randomUUID();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ContestRepository contestRepository;
    @Autowired private ContestRatingChangeRepository ratingChangeRepository;

    @DynamicPropertySource
    static void authService(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.auth-service-url", AuthServiceStub::url);
    }

    @BeforeEach
    void seedRatings() {
        deleteHistoryContests();
        jdbcTemplate.update("DELETE FROM user_ratings");
        AuthServiceStub.reset();
        AuthServiceStub.user(TOP, "an.bui", false);
        AuthServiceStub.user(SECOND, "minh.anh", false);
        AuthServiceStub.user(THIRD, "khoa.tran", false);
        rating(TOP, 1820, 6);
        rating(SECOND, 1640, 3);
        rating(THIRD, 1512, 1);
        rating(NEWCOMER, 1500, 0);
    }

    @AfterEach
    void deleteRatings() {
        deleteHistoryContests();
        jdbcTemplate.update("DELETE FROM user_ratings");
    }

    @Test
    @DisplayName("Xếp theo rating giảm dần, thứ hạng nối tiếp giữa các trang, bỏ người chưa thi")
    void leaderboard_ranksRatedUsers() {
        DocumentContext first = getOk("/api/leaderboard?size=2");
        DocumentContext second = getOk("/api/leaderboard?size=2&page=2");

        assertThat(first.read("$.items[*].username", List.class))
                .containsExactly("an.bui", "minh.anh");
        assertThat(first.read("$.items[*].rank", List.class)).containsExactly(1, 2);
        assertThat(first.read("$.items[0].rating", Integer.class)).isEqualTo(1820);
        assertThat(first.read("$.items[0].contestsPlayed", Integer.class)).isEqualTo(6);
        assertThat(first.read("$.items[0].displayName", String.class)).isEqualTo("Người an.bui");
        assertThat(first.read("$.totalItems", Integer.class)).isEqualTo(3);
        assertThat(second.read("$.items[*].username", List.class)).containsExactly("khoa.tran");
        assertThat(second.read("$.items[0].rank", Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("Rating của một người: hạng tính theo người có rating cao hơn; chưa thi thì rỗng")
    void summary_ranksOneUser() {
        DocumentContext second = getOk("/api/leaderboard/" + SECOND);
        DocumentContext newcomer = getOk("/api/leaderboard/" + NEWCOMER);

        assertThat(second.read("$.rating", Integer.class)).isEqualTo(1640);
        assertThat(second.read("$.rank", Integer.class)).isEqualTo(2);
        assertThat(second.read("$.contestsPlayed", Integer.class)).isEqualTo(3);
        assertThat(newcomer.read("$.rating", Integer.class)).isNull();
        assertThat(newcomer.read("$.rank", Integer.class)).isNull();
        assertThat(
                        getOk("/api/leaderboard/" + UUID.randomUUID())
                                .read("$.contestsPlayed", Integer.class))
                .isZero();
    }

    @Test
    @DisplayName("Lịch sử rating: mỗi contest đã thi một mốc, contest kết thúc trước đứng trước")
    void history_listsRatingAfterEachContest() {
        Contest later =
                contest("lich-su-rating-2", "SQL Cup 2", Instant.parse("2026-10-04T13:00:00Z"));
        Contest earlier =
                contest("lich-su-rating-1", "SQL Cup 1", Instant.parse("2026-09-06T13:00:00Z"));
        ratingChangeRepository.save(
                ContestRatingChange.of(
                        later.getId(), "minh.anh", new Change(SECOND, 4, 1560, 1640, 80)));
        ratingChangeRepository.save(
                ContestRatingChange.of(
                        earlier.getId(), "minh.anh", new Change(SECOND, 12, 1500, 1560, 60)));
        ratingChangeRepository.save(
                ContestRatingChange.of(
                        earlier.getId(), "an.bui", new Change(TOP, 1, 1500, 1620, 120)));

        DocumentContext history = getOk("/api/leaderboard/" + SECOND + "/history");

        assertThat(history.read("$.items[*].contestSlug", List.class))
                .containsExactly("lich-su-rating-1", "lich-su-rating-2");
        assertThat(history.read("$.items[*].rating", List.class)).containsExactly(1560, 1640);
        assertThat(history.read("$.items[*].rank", List.class)).containsExactly(12, 4);
        assertThat(history.read("$.items[1].contestTitle", String.class)).isEqualTo("SQL Cup 2");
        assertThat(history.read("$.items[1].finishedAt", String.class))
                .isEqualTo("2026-10-04T14:30:00Z");
        assertThat(getOk("/api/leaderboard/" + NEWCOMER + "/history").read("$.items", List.class))
                .isEmpty();
    }

    @Test
    @DisplayName("Khách xem được; kích thước trang quá lớn thì 400")
    void leaderboard_isPublicAndValidated() {
        assertThat(
                        restTemplate
                                .getForEntity("/api/leaderboard?size=101", String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void deleteHistoryContests() {
        jdbcTemplate.update(
                "DELETE r FROM contest_rating_changes r JOIN contests c ON c.id = r.contest_id"
                        + " WHERE c.slug LIKE 'lich-su-rating-%'");
        jdbcTemplate.update("DELETE FROM contests WHERE slug LIKE 'lich-su-rating-%'");
    }

    private Contest contest(String slug, String title, Instant startAt) {
        return contestRepository.save(
                Contest.schedule(
                        new ContestSchedule(slug, title, "Mô tả", startAt, 90, 0, 20, true)));
    }

    private void rating(UUID userId, int rating, int contestsPlayed) {
        jdbcTemplate.update(
                "INSERT INTO user_ratings (id, user_id, rating, contests_played, version,"
                        + " created_at, updated_at) VALUES (UUID_TO_BIN(UUID()), UUID_TO_BIN(?),"
                        + " ?, ?, 0, NOW(6), NOW(6))",
                userId.toString(),
                rating,
                contestsPlayed);
    }

    private DocumentContext getOk(String path) {
        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody());
    }
}
