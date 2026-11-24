package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.common.persistence.UuidV7;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class UserSubmissionApiIntegrationTest extends ContainerSupport {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID PROBLEM = UUID.randomUUID();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM submissions");
    }

    @Test
    @DisplayName(
            "Khách xem bài nộp luyện tập đã chấm của người khác theo trang; bỏ bài thi, SE, đang"
                    + " chấm, đã xoá")
    void submissions_listsPublicPracticeSubmissionsByPage() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UUID oldest = store(USER, "WA", "COMPLETED", now.minus(4, ChronoUnit.HOURS));
        UUID middle = store(USER, "AC", "COMPLETED", now.minus(3, ChronoUnit.HOURS));
        UUID newest = store(USER, "RE", "COMPLETED", now.minus(2, ChronoUnit.HOURS));
        store(USER, "SE", "COMPLETED", now.minus(1, ChronoUnit.HOURS));
        store(USER, null, "PENDING", now.minus(50, ChronoUnit.MINUTES));
        UUID contest = store(USER, "AC", "COMPLETED", now.minus(40, ChronoUnit.MINUTES));
        jdbcTemplate.update(
                "UPDATE submissions SET contest_id = UUID_TO_BIN(UUID()) WHERE id = UUID_TO_BIN(?)",
                contest.toString());
        UUID deleted = store(USER, "AC", "COMPLETED", now.minus(30, ChronoUnit.MINUTES));
        jdbcTemplate.update(
                "UPDATE submissions SET deleted_at = NOW(6) WHERE id = UUID_TO_BIN(?)",
                deleted.toString());
        store(UUID.randomUUID(), "AC", "COMPLETED", now);

        DocumentContext first = getOk("/api/submissions/users/" + USER + "?size=2");
        DocumentContext second =
                getOk(
                        "/api/submissions/users/"
                                + USER
                                + "?size=2&cursor="
                                + first.read("$.nextCursor", String.class));

        assertThat(first.read("$.items[*].id", List.class))
                .containsExactly(newest.toString(), middle.toString());
        assertThat(first.read("$.items[0].verdict", String.class)).isEqualTo("RE");
        assertThat(first.read("$.items[1].problemSlug", String.class)).isEqualTo("tim-luong");
        assertThat(first.read("$.items[1].problemNumber", Integer.class)).isEqualTo(12);
        assertThat(first.read("$.items[1].submittedAt", String.class)).isNotBlank();
        assertThat(first.read("$.items[0]", Map.class)).doesNotContainKey("code");
        assertThat(second.read("$.items[*].id", List.class)).containsExactly(oldest.toString());
        assertThat(second.read("$.nextCursor", String.class)).isNull();
    }

    @Test
    @DisplayName("Lịch hoạt động công khai: đếm mọi bài nộp trong năm qua theo ngày giờ Việt Nam")
    void activity_countsSubmissionsInLastYear() {
        Instant now = Instant.now();
        store(USER, "WA", "COMPLETED", now.minus(1, ChronoUnit.DAYS));
        store(USER, "AC", "COMPLETED", now.minus(1, ChronoUnit.DAYS));
        UUID contest = store(USER, "AC", "COMPLETED", now.minus(2, ChronoUnit.DAYS));
        jdbcTemplate.update(
                "UPDATE submissions SET contest_id = UUID_TO_BIN(UUID()) WHERE id = UUID_TO_BIN(?)",
                contest.toString());
        store(USER, "AC", "COMPLETED", now.minus(400, ChronoUnit.DAYS));
        store(UUID.randomUUID(), "AC", "COMPLETED", now);

        DocumentContext activity = getOk("/api/submissions/users/" + USER + "/activity");

        assertThat(activity.read("$.totalSubmissions", Integer.class)).isEqualTo(3);
        assertThat(activity.read("$.days[*].count", List.class)).containsExactly(1, 2);
    }

    @Test
    @DisplayName("Kích thước trang ngoài 1 đến 20 hoặc cursor hỏng trả 400")
    void submissions_validatesPaging() {
        assertThat(
                        restTemplate
                                .getForEntity(
                                        "/api/submissions/users/" + USER + "?size=21", String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(
                        restTemplate
                                .getForEntity(
                                        "/api/submissions/users/" + USER + "?cursor=@@",
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private UUID store(UUID userId, String verdict, String status, Instant createdAt) {
        UUID id = UuidV7.generate(createdAt.toEpochMilli());
        jdbcTemplate.update(
                "INSERT INTO submissions (id, user_id, problem_id, problem_revision_id,"
                    + " problem_number, problem_slug, problem_title, dialect, code, status,"
                    + " verdict, runtime_ms, judged_testcases, total_testcases, attempts, version,"
                    + " created_at, updated_at) VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?),"
                    + " UUID_TO_BIN(?), UUID_TO_BIN(UUID()), 12, 'tim-luong', 'Tìm lương',"
                    + " 'POSTGRESQL', 'SELECT 1', ?, ?, 15, 2, 2, 1, 0, ?, NOW(6))",
                id.toString(),
                userId.toString(),
                PROBLEM.toString(),
                status,
                verdict,
                Timestamp.from(createdAt));
        return id;
    }

    private DocumentContext getOk(String path) {
        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody());
    }
}
