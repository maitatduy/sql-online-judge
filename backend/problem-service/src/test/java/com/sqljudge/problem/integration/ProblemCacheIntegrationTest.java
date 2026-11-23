package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.sqljudge.problem.entity.Tag;
import com.sqljudge.problem.repository.DatasetRepository;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.TagRepository;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.cache.type=redis")
@Import(TestJwts.DecoderConfig.class)
class ProblemCacheIntegrationTest extends ContainerSupport {

    private static final String SLUG = "highest-salary-per-department";
    private static final String DETAIL_KEY = "sqljudge:problem:detail:" + SLUG;
    private static final String TAGS_KEY = "sqljudge:problem:tags";
    private static final long DETAIL_TTL_SECONDS = 600;
    private static final long TAGS_TTL_SECONDS = 3600;
    private static final String CONTEST_STATEMENT_KEY_PREFIX =
            "sqljudge:problem:contest-statement:";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String INTERNAL_TOKEN = "test-internal-token";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private ProblemRepository problemRepository;

    @Autowired private TagRepository tagRepository;

    @Autowired private DatasetRepository datasetRepository;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private StringRedisTemplate redisTemplate;

    @BeforeEach
    void seedProblem() {
        flushCache();
        Tag join = new Tag();
        join.setSlug("join");
        join.setName("JOIN");
        tagRepository.save(join);
        problemRepository.save(
                ProblemFixtures.publishedProblem(
                        SLUG,
                        184,
                        datasetRepository.save(ProblemFixtures.companyDataset("cong-ty"))));
    }

    @AfterEach
    void cleanUp() {
        flushCache();
        jdbcTemplate.update("UPDATE problems SET published_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
        jdbcTemplate.update("DELETE FROM tags");
    }

    @Test
    @DisplayName("Chi tiết bài được cache 10 phút theo slug, lần sau đọc từ Redis")
    void detail_isCachedBySlugForTenMinutes() {
        String first = restTemplate.getForObject("/api/problems/" + SLUG, String.class);
        jdbcTemplate.update("UPDATE problems SET title = 'Tên đã đổi' WHERE slug = ?", SLUG);

        String second = restTemplate.getForObject("/api/problems/" + SLUG, String.class);

        assertThat(JsonPath.parse(second).read("$.title", String.class))
                .isEqualTo("Lương cao nhất mỗi phòng ban");
        assertThat(second).isEqualTo(first);
        assertThat(redisTemplate.getExpire(DETAIL_KEY, TimeUnit.SECONDS))
                .isBetween(1L, DETAIL_TTL_SECONDS);
    }

    @Test
    @DisplayName("Bài không tồn tại không bị cache")
    void detail_notFound_isNotCached() {
        restTemplate.getForObject("/api/problems/khong-co-bai-nay", String.class);

        assertThat(redisTemplate.hasKey("sqljudge:problem:detail:khong-co-bai-nay")).isFalse();
    }

    @Test
    @DisplayName("Danh sách tag được cache 1 giờ")
    void tags_areCachedForOneHour() {
        restTemplate.getForObject("/api/tags", String.class);
        jdbcTemplate.update("UPDATE tags SET name = 'Phép nối' WHERE slug = 'join'");

        String cached = restTemplate.getForObject("/api/tags", String.class);

        assertThat(cached).contains("\"name\":\"JOIN\"");
        assertThat(redisTemplate.getExpire(TAGS_KEY, TimeUnit.SECONDS))
                .isBetween(1L, TAGS_TTL_SECONDS);
    }

    @Test
    @DisplayName("ADMIN xoá bài đã xuất bản thì cache chi tiết bị xoá, trang chi tiết trả 404")
    void delete_publishedProblemByAdmin_evictsDetailCache() {
        restTemplate.getForObject("/api/problems/" + SLUG, String.class);
        String problemId =
                jdbcTemplate.queryForObject(
                        "SELECT BIN_TO_UUID(id) FROM problems WHERE slug = ?", String.class, SLUG);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwts.token(UUID.randomUUID(), "ADMIN"));

        restTemplate.exchange(
                "/api/studio/problems/" + problemId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class);

        assertThat(redisTemplate.hasKey(DETAIL_KEY)).isFalse();
        assertThat(restTemplate.getForEntity("/api/problems/" + SLUG, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Đề contest được cache 10 phút theo id bài, lần sau đọc từ Redis")
    void contestStatement_isCachedByProblemIdForTenMinutes() {
        String problemId = problemId();
        String first = contestStatement(problemId);
        jdbcTemplate.update("UPDATE problems SET title = 'Tên đã đổi' WHERE slug = ?", SLUG);

        String second = contestStatement(problemId);

        assertThat(JsonPath.parse(second).read("$.title", String.class))
                .isEqualTo("Lương cao nhất mỗi phòng ban");
        assertThat(second).isEqualTo(first);
        assertThat(
                        redisTemplate.getExpire(
                                CONTEST_STATEMENT_KEY_PREFIX + problemId, TimeUnit.SECONDS))
                .isBetween(1L, DETAIL_TTL_SECONDS);
    }

    @Test
    @DisplayName("ADMIN xoá bài thì cache đề contest của bài bị xoá")
    void delete_publishedProblemByAdmin_evictsContestStatementCache() {
        String problemId = problemId();
        contestStatement(problemId);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwts.token(UUID.randomUUID(), "ADMIN"));

        restTemplate.exchange(
                "/api/studio/problems/" + problemId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class);

        assertThat(redisTemplate.hasKey(CONTEST_STATEMENT_KEY_PREFIX + problemId)).isFalse();
    }

    private String problemId() {
        return jdbcTemplate.queryForObject(
                "SELECT BIN_TO_UUID(id) FROM problems WHERE slug = ?", String.class, SLUG);
    }

    private String contestStatement(String problemId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(INTERNAL_TOKEN_HEADER, INTERNAL_TOKEN);
        return restTemplate
                .exchange(
                        "/internal/problems/" + problemId + "/contest-statement",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class)
                .getBody();
    }

    private void flushCache() {
        redisTemplate.delete(redisTemplate.keys("sqljudge:problem:*"));
    }
}
