package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
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
class SubmissionCodeApiIntegrationTest extends ContainerSupport {

    private static final String PROBLEM_ID = "0192f3d0-0000-7000-8000-000000000005";
    private static final String CODE = "SELECT name\nFROM departments;";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM submissions");
    }

    @Test
    @DisplayName("Chủ bài nộp xem lại được mã và dialect đã nộp")
    void code_ownSubmission_returnsCode() {
        String id = storeSubmission(TestJwts.LEARNER_ID);

        ResponseEntity<String> response = getCode(id, TestJwts.learnerToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext body = JsonPath.parse(response.getBody());
        assertThat(body.read("$.id", String.class)).isEqualTo(id);
        assertThat(body.read("$.dialect", String.class)).isEqualTo("MYSQL");
        assertThat(body.read("$.code", String.class)).isEqualTo(CODE);
    }

    @Test
    @DisplayName("Mã bài nộp của người khác, đã xoá hoặc không tồn tại trả 404")
    void code_foreignDeletedOrUnknown_isNotFound() {
        String foreign = storeSubmission(UUID.randomUUID());
        String deleted = storeSubmission(TestJwts.LEARNER_ID);
        jdbcTemplate.update(
                "UPDATE submissions SET deleted_at = NOW(6) WHERE id = UUID_TO_BIN(?)", deleted);

        ResponseEntity<String> response = getCode(foreign, TestJwts.learnerToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("SUBMISSION_NOT_FOUND");
        assertThat(response.getBody()).doesNotContain("departments");
        assertThat(getCode(deleted, TestJwts.learnerToken()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getCode(UUID.randomUUID().toString(), TestJwts.learnerToken()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Chưa đăng nhập trả 401, id không phải UUID trả 400")
    void code_unauthenticatedOrMalformedId_isRejected() {
        String id = storeSubmission(TestJwts.LEARNER_ID);

        assertThat(getCode(id, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getCode("not-a-uuid", TestJwts.learnerToken()).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private String storeSubmission(UUID userId) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO submissions (id, user_id, problem_id, problem_revision_id, dialect,"
                        + " code, status, verdict, judged_testcases, total_testcases, attempts,"
                        + " version, created_at, updated_at) VALUES (UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(UUID()), 'MYSQL', ?,"
                        + " 'COMPLETED', 'WA', 3, 3, 1, 0, NOW(6), NOW(6))",
                id,
                userId.toString(),
                PROBLEM_ID,
                CODE);
        return id;
    }

    private ResponseEntity<String> getCode(String id, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        if (bearer != null) {
            headers.setBearerAuth(bearer);
        }
        return restTemplate.exchange(
                "/api/submissions/" + id + "/code",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }
}
