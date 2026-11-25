package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
class SubmissionStatsApiIntegrationTest extends ContainerSupport {

    private static final UUID FIRST_PROBLEM = UUID.randomUUID();
    private static final UUID SECOND_PROBLEM = UUID.randomUUID();
    private static final UUID THIRD_PROBLEM = UUID.randomUUID();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM submissions");
    }

    @Test
    @DisplayName("Tổng hợp đếm số bài đã giải, số bài giải trong 7 ngày và số lần thử")
    void summary_countsOwnSolvesAndAttempts() {
        store(TestJwts.LEARNER_ID, FIRST_PROBLEM, "WA", "NOW(6) - INTERVAL 20 DAY");
        store(TestJwts.LEARNER_ID, FIRST_PROBLEM, "AC", "NOW(6) - INTERVAL 20 DAY");
        store(TestJwts.LEARNER_ID, SECOND_PROBLEM, "AC", "NOW(6) - INTERVAL 2 DAY");
        store(TestJwts.LEARNER_ID, THIRD_PROBLEM, "RE", "NOW(6)");
        store(UUID.randomUUID(), THIRD_PROBLEM, "AC", "NOW(6)");
        UUID deleted = store(TestJwts.LEARNER_ID, THIRD_PROBLEM, "AC", "NOW(6)");
        jdbcTemplate.update(
                "UPDATE submissions SET deleted_at = NOW(6) WHERE id = UUID_TO_BIN(?)",
                deleted.toString());

        DocumentContext summary = get("/api/submissions/summary");

        assertThat(summary.read("$.attemptCount", Integer.class)).isEqualTo(4);
        assertThat(summary.read("$.solvedCount", Integer.class)).isEqualTo(2);
        assertThat(summary.read("$.solvedThisWeek", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Số lần thử bỏ bài nộp sau lần AC đầu, gộp code trùng, bỏ bài chưa chấm xong và SE")
    void summary_attemptsIgnoreResubmissionsDuplicatesPendingAndSystemErrors() {
        storeCode(FIRST_PROBLEM, "WA", "SELECT 1", "NOW(6) - INTERVAL 5 HOUR");
        storeCode(FIRST_PROBLEM, "WA", "SELECT 1", "NOW(6) - INTERVAL 4 HOUR");
        storeCode(FIRST_PROBLEM, "WA", "SELECT 2", "NOW(6) - INTERVAL 3 HOUR");
        storeCode(FIRST_PROBLEM, "AC", "SELECT 3", "NOW(6) - INTERVAL 2 HOUR");
        storeCode(FIRST_PROBLEM, "AC", "SELECT 3", "NOW(6) - INTERVAL 1 HOUR");
        storeCode(FIRST_PROBLEM, "WA", "SELECT 4", "NOW(6)");
        storeCode(SECOND_PROBLEM, "SE", "SELECT 5", "NOW(6)");
        storeCode(SECOND_PROBLEM, "WA", "SELECT 1", "NOW(6)");
        UUID pending = storeCode(SECOND_PROBLEM, "WA", "SELECT 6", "NOW(6)");
        jdbcTemplate.update(
                "UPDATE submissions SET status = 'PENDING', verdict = NULL"
                        + " WHERE id = UUID_TO_BIN(?)",
                pending.toString());

        DocumentContext summary = get("/api/submissions/summary");

        assertThat(summary.read("$.attemptCount", Integer.class)).isEqualTo(4);
        assertThat(summary.read("$.solvedCount", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Hoạt động gom số bài nộp theo ngày giờ Việt Nam, từ ngày được hỏi")
    void activity_groupsByVietnamDay() {
        store(TestJwts.LEARNER_ID, FIRST_PROBLEM, "WA", "'2026-11-10 18:30:00'");
        store(TestJwts.LEARNER_ID, FIRST_PROBLEM, "AC", "'2026-11-11 03:00:00'");
        store(TestJwts.LEARNER_ID, SECOND_PROBLEM, "AC", "'2026-11-12 09:00:00'");
        store(TestJwts.LEARNER_ID, SECOND_PROBLEM, "AC", "'2026-11-01 09:00:00'");
        store(UUID.randomUUID(), SECOND_PROBLEM, "AC", "'2026-11-12 09:00:00'");

        DocumentContext days = get("/api/submissions/activity?from=2026-11-05");

        assertThat(days.read("$[*].date", List.class)).containsExactly("2026-11-11", "2026-11-12");
        assertThat(days.read("$[*].count", List.class)).containsExactly(2, 1);
    }

    @Test
    @DisplayName("Khách nhận 401, khoảng thời gian quá một năm trả 400")
    void stats_rejectAnonymousAndHugeRanges() {
        assertThat(
                        restTemplate
                                .getForEntity("/api/submissions/summary", String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange("/api/submissions/activity?from=2020-01-01").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Bài nộp gần đây của chính mình, mới nhất trước, kèm số, slug và tên bài")
    void recent_returnsOwnNewestSubmissionsWithProblem() {
        store(TestJwts.LEARNER_ID, FIRST_PROBLEM, "WA", "NOW(6) - INTERVAL 3 HOUR");
        UUID middle = store(TestJwts.LEARNER_ID, FIRST_PROBLEM, "AC", "NOW(6) - INTERVAL 2 HOUR");
        UUID newest = store(TestJwts.LEARNER_ID, SECOND_PROBLEM, "RE", "NOW(6) - INTERVAL 1 HOUR");
        store(UUID.randomUUID(), SECOND_PROBLEM, "AC", "NOW(6)");
        jdbcTemplate.update(
                "UPDATE submissions SET problem_number = 184,"
                        + " problem_slug = 'highest-salary-per-department',"
                        + " problem_title = 'Lương cao nhất mỗi phòng ban'"
                        + " WHERE problem_id = UUID_TO_BIN(?)",
                FIRST_PROBLEM.toString());

        DocumentContext recent = get("/api/submissions/recent?size=2");

        assertThat(recent.read("$[*].id", List.class))
                .containsExactly(newest.toString(), middle.toString());
        assertThat(recent.read("$[0].verdict", String.class)).isEqualTo("RE");
        assertThat(recent.read("$[0].problemTitle", String.class)).isNull();
        assertThat(recent.read("$[1].problemNumber", Integer.class)).isEqualTo(184);
        assertThat(recent.read("$[1].problemSlug", String.class))
                .isEqualTo("highest-salary-per-department");
        assertThat(recent.read("$[1].problemTitle", String.class))
                .isEqualTo("Lương cao nhất mỗi phòng ban");
        assertThat(recent.read("$[1].dialect", String.class)).isEqualTo("POSTGRESQL");
        assertThat(exchange("/api/submissions/recent?size=21").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private UUID storeCode(UUID problemId, String verdict, String code, String createdAt) {
        UUID id = store(TestJwts.LEARNER_ID, problemId, verdict, createdAt);
        jdbcTemplate.update(
                "UPDATE submissions SET code = ?, code_hash = UNHEX(SHA2(?, 256))"
                        + " WHERE id = UUID_TO_BIN(?)",
                code,
                code,
                id.toString());
        return id;
    }

    private UUID store(UUID userId, UUID problemId, String verdict, String createdAt) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO submissions (id, user_id, problem_id, problem_revision_id, dialect,"
                        + " code, status, verdict, runtime_ms, judged_testcases, total_testcases,"
                        + " attempts, version, created_at, updated_at) VALUES (UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(UUID()), 'POSTGRESQL',"
                        + " 'SELECT 1', 'COMPLETED', ?, 15, 2, 2, 1, 0, "
                        + createdAt
                        + ", NOW(6))",
                id.toString(),
                userId.toString(),
                problemId.toString(),
                verdict);
        return id;
    }

    private DocumentContext get(String path) {
        ResponseEntity<String> response = exchange(path);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody());
    }

    private ResponseEntity<String> exchange(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwts.learnerToken());
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
