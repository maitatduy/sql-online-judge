package com.sqljudge.problem.integration;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class StudioProblemEditApiIntegrationTest extends ContainerSupport {

    private static final String PATH = "/api/studio/problems";
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String INSERT_TAG =
            "INSERT INTO tags (id, slug, name, created_at, updated_at)"
                    + " VALUES (UUID_TO_BIN(UUID()), ?, ?, NOW(6), NOW(6))";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private String problemId;

    @BeforeEach
    void createDraft() {
        jdbcTemplate.update(INSERT_TAG, "join", "JOIN");
        jdbcTemplate.update(INSERT_TAG, "window-function", "Window Function");
        problemId =
                JsonPath.parse(
                                send(HttpMethod.POST, PATH, SETTER, "{\"title\":\"Bài thử\"}")
                                        .getBody())
                        .read("$.id", String.class);
    }

    @AfterEach
    void deleteProblems() {
        jdbcTemplate.update("UPDATE problems SET draft_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM tags");
    }

    @Test
    @DisplayName("Chủ bài và quản trị viên đọc được bài nháp, người ra đề khác nhận 404")
    void get_ownerOrAdmin_returnsProblem() {
        DocumentContext problem =
                JsonPath.parse(send(HttpMethod.GET, url(), SETTER, null).getBody());

        assertThat(problem.read("$.title", String.class)).isEqualTo("Bài thử");
        assertThat(problem.read("$.status", String.class)).isEqualTo("DRAFT");
        assertThat(problem.read("$.statementMarkdown", String.class)).isEmpty();
        assertThat(problem.read("$.tags", List.class)).isEmpty();
        assertThat(send(HttpMethod.GET, url(), ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(send(HttpMethod.GET, url(), OTHER_SETTER, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(send(HttpMethod.GET, url(), TestJwts.learnerToken(), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Sửa tên, slug, độ khó, chủ đề và đề bài, chỉ trường gửi lên bị đổi")
    void patch_infoAndStatement_updatesOnlySentFields() {
        ResponseEntity<String> response =
                patch(
                        SETTER,
                        "{\"title\":\"Lương trung vị\",\"slug\":\"median-salary\","
                            + "\"difficulty\":\"HARD\",\"tagSlugs\":[\"window-function\",\"join\"],\"statementMarkdown\":\"Tìm"
                            + " **lương trung vị**.\"}");
        DocumentContext afterTitleOnly =
                JsonPath.parse(patch(SETTER, "{\"title\":\"Lương trung vị mỗi phòng\"}").getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(afterTitleOnly.read("$.title", String.class))
                .isEqualTo("Lương trung vị mỗi phòng");
        assertThat(afterTitleOnly.read("$.slug", String.class)).isEqualTo("median-salary");
        assertThat(afterTitleOnly.read("$.difficulty", String.class)).isEqualTo("HARD");
        assertThat(afterTitleOnly.read("$.tags[*].slug", List.class))
                .containsExactly("join", "window-function");
        assertThat(afterTitleOnly.read("$.statementMarkdown", String.class))
                .isEqualTo("Tìm **lương trung vị**.");
    }

    @Test
    @DisplayName("Slug sai định dạng trả 400, trùng bài khác trả 409 SLUG_TAKEN")
    void patch_invalidOrTakenSlug_isRejected() {
        send(HttpMethod.POST, PATH, OTHER_SETTER, "{\"title\":\"Bài khác\"}");

        ResponseEntity<String> invalid = patch(SETTER, "{\"slug\":\"Lương Cao\"}");
        ResponseEntity<String> taken = patch(SETTER, "{\"slug\":\"bai-khac\"}");

        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(invalid.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("slug");
        assertThat(taken.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(JsonPath.parse(taken.getBody()).read("$.code", String.class))
                .isEqualTo("SLUG_TAKEN");
    }

    @Test
    @DisplayName("Chủ đề không tồn tại hoặc đề bài quá 50 KB trả 400 ở đúng trường")
    void patch_unknownTagOrHugeStatement_returnsFieldError() {
        ResponseEntity<String> unknownTag = patch(SETTER, "{\"tagSlugs\":[\"khong-co\"]}");
        ResponseEntity<String> hugeStatement =
                patch(SETTER, "{\"statementMarkdown\":\"" + "a".repeat(50 * 1024 + 1) + "\"}");

        assertThat(unknownTag.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(unknownTag.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("tagSlugs");
        assertThat(hugeStatement.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.parse(hugeStatement.getBody()).read("$.errors[0].field", String.class))
                .isEqualTo("statementMarkdown");
    }

    @Test
    @DisplayName("Bài đang chờ duyệt không sửa được, người ra đề khác sửa thì nhận 404")
    void patch_lockedOrNotOwned_isRejected() {
        assertThat(patch(OTHER_SETTER, "{\"title\":\"Chiếm bài\"}").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        jdbcTemplate.update("UPDATE problems SET status = 'PENDING_REVIEW'");
        ResponseEntity<String> locked = patch(SETTER, "{\"title\":\"Sửa khi chờ duyệt\"}");

        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(JsonPath.parse(locked.getBody()).read("$.code", String.class))
                .isEqualTo("PROBLEM_LOCKED");
    }

    private String url() {
        return PATH + "/" + problemId;
    }

    private ResponseEntity<String> patch(String token, String body) {
        return send(HttpMethod.PATCH, url(), token, body);
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
