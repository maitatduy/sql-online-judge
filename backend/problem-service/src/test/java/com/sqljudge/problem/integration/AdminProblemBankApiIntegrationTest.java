package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemStats;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.entity.Tag;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.ProblemStatsRepository;
import com.sqljudge.problem.repository.TagRepository;
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
class AdminProblemBankApiIntegrationTest extends ContainerSupport {

    private static final UUID AUTHOR_ID = UUID.fromString("0192f3a0-0000-7000-8000-0000000000ae");
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String BANK = "/api/admin/problems?status=PUBLISHED&sort=number";
    private static final String SUMMARY = "/api/admin/problems/summary";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private ProblemRepository problemRepository;

    @Autowired private TagRepository tagRepository;

    @Autowired private ProblemStatsRepository problemStatsRepository;

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
        topProducts.setVisibility(ProblemVisibility.CONTEST_ONLY);
        Problem hidden = published(4, "hidden-problem", "Bài đang ẩn", Difficulty.EASY, join);
        hidden.setVisibility(ProblemVisibility.HIDDEN);
        Problem draft = published(5, "draft-problem", "Bài nháp", Difficulty.EASY, join);
        draft.setStatus(ProblemStatus.DRAFT);
        draft.setVisibility(null);
        draft.setNumber(null);
        Problem deleted = published(6, "deleted-problem", "Bài đã xoá", Difficulty.EASY, join);
        deleted.setDeletedAt(Instant.parse("2026-11-01T00:00:00Z"));
        problemRepository.saveAll(
                List.of(neverOrdered, highestSalary, topProducts, hidden, draft, deleted));
        problemStatsRepository.saveAll(
                List.of(
                        new ProblemStats(neverOrdered.getId(), 1000, 684, 600),
                        new ProblemStats(highestSalary.getId(), 100, 5, 4),
                        new ProblemStats(topProducts.getId(), 20, 1, 1)));
    }

    @AfterEach
    void deleteProblems() {
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM tags");
    }

    @Test
    @DisplayName("Kho liệt kê mọi bài đã xuất bản theo số bài, kèm tag và số liệu nộp bài")
    void bank_listsPublishedProblemsWithTagsAndStats() {
        DocumentContext page = JsonPath.parse(get(BANK, ADMIN).getBody());

        assertThat(page.read("$.items[*].number", List.class)).containsExactly(1, 2, 3, 4);
        assertThat(page.read("$.items[*].visibility", List.class))
                .containsExactly("PUBLIC", "PUBLIC", "CONTEST_ONLY", "HIDDEN");
        assertThat(page.read("$.items[1].tags[*].slug", List.class)).containsExactly("join");
        assertThat(page.read("$.items[1].submissionCount", Integer.class)).isEqualTo(100);
        assertThat(page.read("$.items[1].acceptedCount", Integer.class)).isEqualTo(5);
        assertThat(page.read("$.items[1].acceptanceRate", Double.class)).isEqualTo(5.0);
        assertThat(page.read("$.items[3].submissionCount", Integer.class)).isZero();
        assertThat(page.read("$.items[3].acceptanceRate", Double.class)).isZero();
        assertThat(page.read("$.totalItems", Integer.class)).isEqualTo(4);
    }

    @Test
    @DisplayName("Lọc theo phạm vi hiển thị, độ khó, tag, tên hoặc số bài")
    void bank_filters() {
        assertThat(numbers("&visibility=HIDDEN")).containsExactly(4);
        assertThat(numbers("&difficulty=EASY")).containsExactly(1, 4);
        assertThat(numbers("&tag=cte")).containsExactly(3);
        assertThat(numbers("&q=lương")).containsExactly(2);
        assertThat(numbers("&q=3")).containsExactly(3);
    }

    @Test
    @DisplayName("Số liệu đếm theo phạm vi và bài AC dưới 10% có ít nhất 50 lượt nộp")
    void summary_countsVisibilityAndLowAcceptance() {
        DocumentContext summary = JsonPath.parse(get(SUMMARY, ADMIN).getBody());

        assertThat(summary.read("$.publicCount", Integer.class)).isEqualTo(2);
        assertThat(summary.read("$.contestOnlyCount", Integer.class)).isEqualTo(1);
        assertThat(summary.read("$.hiddenCount", Integer.class)).isEqualTo(1);
        assertThat(summary.read("$.lowAcceptanceCount", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Kiểu sắp xếp sai trả 400, không phải ADMIN nhận 403, chưa đăng nhập nhận 401")
    void bank_invalidSortOrNonAdmin_isRejected() {
        assertThat(get(BANK.replace("sort=number", "sort=title"), ADMIN).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(get(SUMMARY, SETTER).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(SUMMARY, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get(BANK, SETTER).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private List<Integer> numbers(String filter) {
        return JsonPath.parse(get(BANK + filter, ADMIN).getBody()).read("$.items[*].number");
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
