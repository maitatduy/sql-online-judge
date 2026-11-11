package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.common.persistence.UuidV7;
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
class SubmissionHistoryApiIntegrationTest extends ContainerSupport {

    private static final UUID PROBLEM_ID = UUID.fromString("0192f3d0-0000-7000-8000-000000000009");

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private long clockMillis = System.currentTimeMillis();

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM submissions");
    }

    @Test
    @DisplayName(
            "Lịch sử chỉ gồm bài nộp của chính mình cho bài này, mới nhất trước, phân trang bằng"
                    + " cursor")
    void history_ownSubmissionsNewestFirst_pagedByCursor() {
        UUID oldest = store(TestJwts.LEARNER_ID, PROBLEM_ID, "WA");
        store(UUID.randomUUID(), PROBLEM_ID, "AC");
        store(TestJwts.LEARNER_ID, UUID.randomUUID(), "AC");
        UUID middle = store(TestJwts.LEARNER_ID, PROBLEM_ID, "RE");
        UUID deleted = store(TestJwts.LEARNER_ID, PROBLEM_ID, "AC");
        jdbcTemplate.update(
                "UPDATE submissions SET deleted_at = NOW(6) WHERE id = UUID_TO_BIN(?)",
                deleted.toString());
        UUID newest = store(TestJwts.LEARNER_ID, PROBLEM_ID, "AC");

        ResponseEntity<String> first = history("?size=2", TestJwts.learnerToken());

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext firstPage = JsonPath.parse(first.getBody());
        assertThat(firstPage.read("$.items[*].id", List.class))
                .containsExactly(newest.toString(), middle.toString());
        assertThat(firstPage.read("$.items[0].verdict", String.class)).isEqualTo("AC");
        assertThat(firstPage.read("$.items[0].status", String.class)).isEqualTo("COMPLETED");
        assertThat(firstPage.read("$.items[0].dialect", String.class)).isEqualTo("POSTGRESQL");
        assertThat(firstPage.read("$.items[0].runtimeMs", Integer.class)).isEqualTo(15);
        assertThat(firstPage.read("$.items[0].submittedAt", String.class)).isNotBlank();
        assertThat(first.getBody()).doesNotContain("SELECT 1");
        String cursor = firstPage.read("$.nextCursor", String.class);
        assertThat(cursor).isNotBlank();

        DocumentContext secondPage =
                JsonPath.parse(
                        history("?size=2&cursor=" + cursor, TestJwts.learnerToken()).getBody());
        assertThat(secondPage.read("$.items[*].id", List.class)).containsExactly(oldest.toString());
        assertThat(secondPage.read("$.nextCursor", String.class)).isNull();
    }

    @Test
    @DisplayName("Chưa nộp lần nào thì trả danh sách rỗng")
    void history_noSubmissions_isEmpty() {
        DocumentContext page = JsonPath.parse(history("", TestJwts.learnerToken()).getBody());

        assertThat(page.read("$.items", List.class)).isEmpty();
        assertThat(page.read("$.nextCursor", String.class)).isNull();
    }

    @Test
    @DisplayName("Cursor hỏng hoặc kích thước trang sai trả 400, chưa đăng nhập trả 401")
    void history_invalidParametersOrAnonymous_isRejected() {
        ResponseEntity<String> badCursor = history("?cursor=khong-hop-le", TestJwts.learnerToken());
        assertThat(badCursor.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(badCursor.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("cursor");
        assertThat(history("?size=0", TestJwts.learnerToken()).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(history("?size=101", TestJwts.learnerToken()).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(history("", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private UUID store(UUID userId, UUID problemId, String verdict) {
        UUID id = UuidV7.generate(clockMillis++);
        jdbcTemplate.update(
                "INSERT INTO submissions (id, user_id, problem_id, problem_revision_id, dialect,"
                        + " code, status, verdict, runtime_ms, judged_testcases, total_testcases,"
                        + " attempts, version, created_at, updated_at) VALUES (UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(UUID()), 'POSTGRESQL',"
                        + " 'SELECT 1', 'COMPLETED', ?, 15, 2, 2, 1, 0, NOW(6), NOW(6))",
                id.toString(),
                userId.toString(),
                problemId.toString(),
                verdict);
        return id;
    }

    private ResponseEntity<String> history(String query, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        if (bearer != null) {
            headers.setBearerAuth(bearer);
        }
        return restTemplate.exchange(
                "/api/problems/" + PROBLEM_ID + "/submissions" + query,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }
}
