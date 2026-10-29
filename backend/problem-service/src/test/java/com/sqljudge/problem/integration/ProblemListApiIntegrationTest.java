package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemProgressStatus;
import com.sqljudge.problem.entity.ProblemStats;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.entity.Tag;
import com.sqljudge.problem.entity.UserProblemProgress;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.ProblemStatsRepository;
import com.sqljudge.problem.repository.TagRepository;
import com.sqljudge.problem.repository.UserProblemProgressRepository;
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
class ProblemListApiIntegrationTest extends ContainerSupport {

    private static final UUID AUTHOR_ID = UUID.fromString("0192f3a0-0000-7000-8000-0000000000ad");

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private ProblemRepository problemRepository;

    @Autowired private TagRepository tagRepository;

    @Autowired private ProblemStatsRepository problemStatsRepository;

    @Autowired private UserProblemProgressRepository userProblemProgressRepository;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedProblems() {
        Tag join = tag("join", "JOIN");
        Tag cte = tag("cte", "CTE");
        Problem neverOrdered =
                published(
                        1,
                        "customers-who-never-order",
                        "Khách hàng chưa từng đặt hàng",
                        Difficulty.EASY,
                        join);
        Problem highestSalary =
                published(
                        2,
                        "highest-salary-per-department",
                        "Lương cao nhất mỗi phòng ban",
                        Difficulty.MEDIUM,
                        join);
        Problem topProducts =
                published(
                        3, "top-three-products", "Top 3 sản phẩm mỗi tháng", Difficulty.HARD, cte);
        Problem hidden = published(4, "hidden-problem", "Bài đang ẩn", Difficulty.EASY, join);
        hidden.setVisibility(ProblemVisibility.HIDDEN);
        Problem draft = published(5, "draft-problem", "Bài nháp", Difficulty.EASY, join);
        draft.setStatus(ProblemStatus.DRAFT);
        draft.setVisibility(null);
        draft.setNumber(null);
        problemRepository.saveAll(List.of(neverOrdered, highestSalary, topProducts, hidden, draft));
        problemStatsRepository.saveAll(
                List.of(
                        new ProblemStats(neverOrdered.getId(), 1000, 684, 24112),
                        new ProblemStats(highestSalary.getId(), 1000, 412, 12380)));
        userProblemProgressRepository.saveAll(
                List.of(
                        new UserProblemProgress(
                                TestJwts.LEARNER_ID,
                                highestSalary.getId(),
                                ProblemProgressStatus.SOLVED),
                        new UserProblemProgress(
                                TestJwts.LEARNER_ID,
                                topProducts.getId(),
                                ProblemProgressStatus.ATTEMPTED),
                        new UserProblemProgress(
                                UUID.randomUUID(),
                                neverOrdered.getId(),
                                ProblemProgressStatus.SOLVED)));
    }

    @AfterEach
    void deleteProblems() {
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM tags");
    }

    @Test
    @DisplayName("Khách chỉ thấy bài đã xuất bản và công khai, xếp theo số bài, kèm thống kê")
    void list_anonymous_returnsPublishedPublicProblems() {
        DocumentContext page = getOk("/api/problems", null);

        assertThat(page.read("$.items[*].number", List.class)).containsExactly(1, 2, 3);
        assertThat(page.read("$.page", Integer.class)).isEqualTo(1);
        assertThat(page.read("$.size", Integer.class)).isEqualTo(20);
        assertThat(page.read("$.totalItems", Integer.class)).isEqualTo(3);
        assertThat(page.read("$.totalPages", Integer.class)).isEqualTo(1);
        assertThat(page.read("$.items[1].slug", String.class))
                .isEqualTo("highest-salary-per-department");
        assertThat(page.read("$.items[1].difficulty", String.class)).isEqualTo("MEDIUM");
        assertThat(page.read("$.items[1].acceptanceRate", Double.class)).isEqualTo(41.2);
        assertThat(page.read("$.items[1].solvedCount", Integer.class)).isEqualTo(12380);
        assertThat(page.read("$.items[1].tags[0].name", String.class)).isEqualTo("JOIN");
        assertThat(page.read("$.items[2].acceptanceRate", Double.class)).isZero();
        assertThat(page.read("$.items[*].progress", List.class)).containsOnlyNulls();
    }

    @Test
    @DisplayName("Lọc theo độ khó, tag, tên không phân biệt dấu và hoa thường, hoặc số bài")
    void list_withFilters_returnsMatchingProblems() {
        assertThat(numbers("/api/problems?difficulty=MEDIUM", null)).containsExactly(2);
        assertThat(numbers("/api/problems?tag=cte", null)).containsExactly(3);
        assertThat(numbers("/api/problems?q=LUONG cao", null)).containsExactly(2);
        assertThat(numbers("/api/problems?q=1", null)).containsExactly(1);
        assertThat(numbers("/api/problems?q=100%", null)).isEmpty();
    }

    @Test
    @DisplayName("Phân trang theo page bắt đầu từ 1 và size")
    void list_secondPage_returnsRemainingProblems() {
        DocumentContext page = getOk("/api/problems?page=2&size=2", null);

        assertThat(page.read("$.items[*].number", List.class)).containsExactly(3);
        assertThat(page.read("$.totalPages", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("Người đã đăng nhập thấy và lọc được tiến độ của chính mình")
    void list_signedIn_returnsOwnProgress() {
        String token = TestJwts.learnerToken();

        assertThat(getOk("/api/problems", token).read("$.items[*].progress", List.class))
                .containsExactly("NOT_STARTED", "SOLVED", "ATTEMPTED");
        assertThat(numbers("/api/problems?progress=SOLVED", token)).containsExactly(2);
        assertThat(numbers("/api/problems?progress=NOT_STARTED", token)).containsExactly(1);
    }

    @Test
    @DisplayName("Khách gửi bộ lọc tiến độ thì bộ lọc bị bỏ qua")
    void list_anonymousWithProgressFilter_ignoresFilter() {
        assertThat(numbers("/api/problems?progress=SOLVED", null)).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Tham số phân trang hoặc độ khó sai trả 400")
    void list_invalidParameters_returnsBadRequest() {
        ResponseEntity<String> zeroPage = get("/api/problems?page=0", null);
        ResponseEntity<String> hugeSize = get("/api/problems?size=101", null);
        ResponseEntity<String> unknownDifficulty = get("/api/problems?difficulty=ABC", null);

        assertThat(zeroPage.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(zeroPage.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("page");
        assertThat(hugeSize.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unknownDifficulty.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Token giả trả 401 kể cả ở endpoint công khai")
    void list_invalidToken_returnsUnauthorized() {
        assertThat(get("/api/problems", "not-a-jwt").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private List<Integer> numbers(String path, String token) {
        return getOk(path, token).read("$.items[*].number", List.class);
    }

    private DocumentContext getOk(String path, String token) {
        ResponseEntity<String> response = get(path, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody());
    }

    private ResponseEntity<String> get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private Tag tag(String slug, String name) {
        Tag tag = new Tag();
        tag.setSlug(slug);
        tag.setName(name);
        return tagRepository.save(tag);
    }

    private static Problem published(
            int number, String slug, String title, Difficulty difficulty, Tag tag) {
        Problem problem = new Problem();
        problem.setNumber(number);
        problem.setSlug(slug);
        problem.setTitle(title);
        problem.setDifficulty(difficulty);
        problem.setStatus(ProblemStatus.PUBLISHED);
        problem.setVisibility(ProblemVisibility.PUBLIC);
        problem.setAuthorId(AUTHOR_ID);
        problem.setStatementMarkdown("");
        problem.setStarterCode("");
        problem.setTimeLimitMs(2000);
        problem.getTags().add(tag);
        return problem;
    }
}
