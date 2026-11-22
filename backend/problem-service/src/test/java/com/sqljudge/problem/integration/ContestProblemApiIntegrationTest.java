package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.problem.event.ContestFinishedEvent;
import com.sqljudge.problem.service.ContestProblemOpeningService;
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
class ContestProblemApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String PUBLISHED_TOPIC = "sqljudge.problem.published";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String INTERNAL_TOKEN = "test-internal-token";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ContestProblemOpeningService openingService;

    private StudioFixtures studio;
    private String problemId;

    @BeforeEach
    void submitProblemForReview() {
        studio = new StudioFixtures(restTemplate, SETTER);
        problemId = ReviewFixtures.submitForReview(studio, jdbcTemplate, "[]");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM processed_events");
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM answer_checks");
        jdbcTemplate.update(
                "UPDATE problems SET draft_revision_id = NULL, published_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Duyệt cho contest thì bài được chốt nhưng ẩn khỏi trang công khai, chưa có số")
    void approveForContest_keepsProblemPrivate() {
        ResponseEntity<String> response = approveForContest(ADMIN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext problem = JsonPath.parse(response.getBody());
        assertThat(problem.read("$.status", String.class)).isEqualTo("PUBLISHED");
        assertThat(problem.read("$.visibility", String.class)).isEqualTo("CONTEST_ONLY");
        assertThat(problem.read("$.number", Integer.class)).isNull();
        assertThat(problem.read("$.latestReview.decision", String.class)).isEqualTo("APPROVED");
        assertThat(publishedEventCount()).isZero();
        String slug = problem.read("$.slug", String.class);
        assertThat(restTemplate.getForEntity("/api/problems/" + slug, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForObject("/api/problems", String.class)).doesNotContain(slug);
    }

    @Test
    @DisplayName("Admin lọc được bài đã xuất bản theo phạm vi: chỉ cho contest hoặc công khai")
    void adminList_filtersByVisibility() {
        approveForContest(ADMIN);
        String published = "/api/admin/problems?status=PUBLISHED&visibility=";

        DocumentContext contestOnly =
                JsonPath.parse(
                        studio.send(HttpMethod.GET, published + "CONTEST_ONLY", null, ADMIN)
                                .getBody());
        DocumentContext publicOnes =
                JsonPath.parse(
                        studio.send(HttpMethod.GET, published + "PUBLIC", null, ADMIN).getBody());

        assertThat(contestOnly.read("$.items[*].id", List.class)).containsExactly(problemId);
        assertThat(contestOnly.read("$.items[0].visibility", String.class))
                .isEqualTo("CONTEST_ONLY");
        assertThat(publicOnes.read("$.items", List.class)).isEmpty();
    }

    @Test
    @DisplayName("Chỉ ADMIN duyệt cho contest được")
    void approveForContest_asSetter_isForbidden() {
        assertThat(approveForContest(SETTER).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("API nội bộ chỉ trả bài dành cho contest trong danh sách id, cần token nội bộ")
    void contestReady_returnsOnlyContestOnlyProblems() {
        approveForContest(ADMIN);
        String unknownId = UUID.randomUUID().toString();
        String path = "/internal/problems/contest-ready?ids=" + problemId + "," + unknownId;

        ResponseEntity<String> response =
                restTemplate.exchange(
                        path, HttpMethod.GET, new HttpEntity<>(internalHeaders()), String.class);
        ResponseEntity<String> anonymous = restTemplate.getForEntity(path, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext body = JsonPath.parse(response.getBody());
        assertThat(body.read("$[*].problemId", List.class)).containsExactly(problemId);
        assertThat(body.read("$[0].title", String.class)).isEqualTo("Phòng ban");
        assertThat(body.read("$[0].difficulty", String.class)).isNotBlank();
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("API nội bộ trả đề cho contest, không lộ số, slug, tag hay thống kê")
    void contestStatement_returnsStatementWithoutPublicIdentity() {
        String path = "/internal/problems/" + problemId + "/contest-statement";
        ResponseEntity<String> beforeApproval =
                restTemplate.exchange(
                        path, HttpMethod.GET, new HttpEntity<>(internalHeaders()), String.class);
        approveForContest(ADMIN);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        path, HttpMethod.GET, new HttpEntity<>(internalHeaders()), String.class);

        assertThat(beforeApproval.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext statement = JsonPath.parse(response.getBody());
        assertThat(statement.read("$.title", String.class)).isEqualTo("Phòng ban");
        assertThat(statement.read("$.statementMarkdown", String.class)).isNotBlank();
        assertThat(statement.read("$.dialects", List.class)).isNotEmpty();
        assertThat(statement.read("$.hiddenTestcaseCount", Integer.class)).isNotNull();
        assertThat(response.getBody())
                .doesNotContain("\"slug\"")
                .doesNotContain("\"number\"")
                .doesNotContain("\"tags\"")
                .doesNotContain("solvedCount");
        assertThat(restTemplate.getForEntity(path, String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Bài dành cho contest có thông tin chấm riêng, không lộ số và slug")
    void contestJudgingInfo_servesContestOnlyProblems() {
        approveForContest(ADMIN);
        String base = "/internal/problems/" + problemId;

        ResponseEntity<String> practice =
                restTemplate.exchange(
                        base + "/judging-info",
                        HttpMethod.GET,
                        new HttpEntity<>(internalHeaders()),
                        String.class);
        ResponseEntity<String> contest =
                restTemplate.exchange(
                        base + "/contest-judging-info",
                        HttpMethod.GET,
                        new HttpEntity<>(internalHeaders()),
                        String.class);

        assertThat(practice.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(contest.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext info = JsonPath.parse(contest.getBody());
        assertThat(info.read("$.problemId", String.class)).isEqualTo(problemId);
        assertThat(info.read("$.revisionId", String.class)).isNotBlank();
        assertThat(info.read("$.title", String.class)).isEqualTo("Phòng ban");
        assertThat(info.read("$.slug", String.class)).isNull();
        assertThat(info.read("$.number", Integer.class)).isNull();
    }

    @Test
    @DisplayName("Contest kết thúc thì bài được mở công khai, có số và phát event một lần")
    void contestFinished_opensProblemsOnce() {
        String slug = JsonPath.parse(approveForContest(ADMIN).getBody()).read("$.slug");
        ContestFinishedEvent event =
                new ContestFinishedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        1,
                        UUID.randomUUID(),
                        List.of(UUID.fromString(problemId)));

        openingService.open(event);
        openingService.open(event);

        DocumentContext detail =
                JsonPath.parse(restTemplate.getForObject("/api/problems/" + slug, String.class));
        assertThat(detail.read("$.number", Integer.class)).isPositive();
        assertThat(publishedEventCount()).isEqualTo(1);
    }

    private ResponseEntity<String> approveForContest(String bearer) {
        return studio.send(
                HttpMethod.POST,
                "/api/admin/problems/" + problemId + "/approve-for-contest",
                null,
                bearer);
    }

    private static HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(INTERNAL_TOKEN_HEADER, INTERNAL_TOKEN);
        return headers;
    }

    private long publishedEventCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE topic = ?", Long.class, PUBLISHED_TOPIC);
    }
}
