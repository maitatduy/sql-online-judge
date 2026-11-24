package com.sqljudge.contest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
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

    @DynamicPropertySource
    static void authService(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.auth-service-url", AuthServiceStub::url);
    }

    @BeforeEach
    void seedRatings() {
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
    @DisplayName("Khách xem được; kích thước trang quá lớn thì 400")
    void leaderboard_isPublicAndValidated() {
        assertThat(
                        restTemplate
                                .getForEntity("/api/leaderboard?size=101", String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
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
