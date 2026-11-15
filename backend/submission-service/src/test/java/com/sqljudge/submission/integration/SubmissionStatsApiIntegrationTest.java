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
    @DisplayName("Tổng hợp đếm bài nộp, lần AC, số bài đã giải và số bài giải trong 7 ngày")
    void summary_countsOwnSubmissions() {
        store(TestJwts.LEARNER_ID, FIRST_PROBLEM, "WA", "NOW(6) - INTERVAL 20 DAY");
        store(TestJwts.LEARNER_ID, FIRST_PROBLEM, "AC", "NOW(6) - INTERVAL 20 DAY");
        store(TestJwts.LEARNER_ID, FIRST_PROBLEM, "AC", "NOW(6) - INTERVAL 1 DAY");
        store(TestJwts.LEARNER_ID, SECOND_PROBLEM, "AC", "NOW(6) - INTERVAL 2 DAY");
        store(TestJwts.LEARNER_ID, THIRD_PROBLEM, "RE", "NOW(6)");
        store(UUID.randomUUID(), THIRD_PROBLEM, "AC", "NOW(6)");
        UUID deleted = store(TestJwts.LEARNER_ID, THIRD_PROBLEM, "AC", "NOW(6)");
        jdbcTemplate.update(
                "UPDATE submissions SET deleted_at = NOW(6) WHERE id = UUID_TO_BIN(?)",
                deleted.toString());

        DocumentContext summary = get("/api/submissions/summary");

        assertThat(summary.read("$.submissionCount", Integer.class)).isEqualTo(5);
        assertThat(summary.read("$.acceptedCount", Integer.class)).isEqualTo(3);
        assertThat(summary.read("$.solvedCount", Integer.class)).isEqualTo(2);
        assertThat(summary.read("$.solvedThisWeek", Integer.class)).isEqualTo(1);
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
