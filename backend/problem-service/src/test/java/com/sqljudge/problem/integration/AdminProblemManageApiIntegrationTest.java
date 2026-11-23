package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.entity.Tag;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.TagRepository;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class AdminProblemManageApiIntegrationTest extends ContainerSupport {

    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final UUID NEW_OWNER_ID = UUID.randomUUID();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID problemId;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.auth-service-url", AuthServiceStub::url);
        registry.add("sqljudge.contest-service-url", ContestServiceStub::url);
    }

    @BeforeEach
    void seedProblem() {
        AuthServiceStub.reset();
        ContestServiceStub.reset();
        tag("join", "JOIN");
        tag("cte", "CTE");
        Problem problem = new Problem();
        problem.setNumber(7);
        problem.setSlug("top-products");
        problem.setTitle("Top sản phẩm");
        problem.setDifficulty(Difficulty.EASY);
        problem.setStatus(ProblemStatus.PUBLISHED);
        problem.setVisibility(ProblemVisibility.CONTEST_ONLY);
        problem.setAuthorId(UUID.randomUUID());
        problem.setStatementMarkdown("");
        problem.setStarterCode("");
        problem.setTimeLimitMs(2000);
        problemId = problemRepository.save(problem).getId();
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM tags");
    }

    @Test
    @DisplayName("Sửa nhanh đổi tên, độ khó và chủ đề; dữ liệu sai trả 400, chủ đề lạ trả 422")
    void quickEdit_updatesTitleDifficultyAndTags() {
        ResponseEntity<String> edited =
                send(
                        HttpMethod.PATCH,
                        "",
                        "{\"title\": \" Top 3 sản phẩm \", \"difficulty\": \"HARD\","
                                + " \"tagSlugs\": [\"cte\", \"join\"]}",
                        ADMIN);

        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(column("title")).isEqualTo("Top 3 sản phẩm");
        assertThat(column("difficulty")).isEqualTo("HARD");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM problem_tags WHERE problem_id ="
                                        + " UUID_TO_BIN(?)",
                                Integer.class,
                                problemId.toString()))
                .isEqualTo(2);
        assertThat(
                        send(
                                        HttpMethod.PATCH,
                                        "",
                                        "{\"title\": \"\", \"difficulty\": \"HARD\", \"tagSlugs\":"
                                                + " []}",
                                        ADMIN)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(
                        code(
                                send(
                                        HttpMethod.PATCH,
                                        "",
                                        "{\"title\": \"A\", \"difficulty\": \"HARD\","
                                                + " \"tagSlugs\": [\"khong-co\"]}",
                                        ADMIN)))
                .isEqualTo("TAG_NOT_FOUND");
    }

    @Test
    @DisplayName("Ẩn rồi hiện lại trả bài về đúng phạm vi cũ; ẩn hai lần trả 422")
    void hideAndRestore_keepPreviousVisibility() {
        assertThat(send(HttpMethod.POST, "/hide", null, ADMIN).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(column("visibility")).isEqualTo("HIDDEN");
        assertThat(code(send(HttpMethod.POST, "/hide", null, ADMIN)))
                .isEqualTo("PROBLEM_VISIBILITY_UNCHANGED");

        assertThat(send(HttpMethod.POST, "/restore", null, ADMIN).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(column("visibility")).isEqualTo("CONTEST_ONLY");
    }

    @Test
    @DisplayName(
            "Bài trong contest chưa kết thúc không ẩn, không xoá được; contest-service lỗi trả 503")
    void hideOrDelete_problemInUnfinishedContest_isBlocked() {
        ContestServiceStub.inUnfinishedContest(problemId);

        ResponseEntity<String> hidden = send(HttpMethod.POST, "/hide", null, ADMIN);
        ResponseEntity<String> deleted = send(HttpMethod.DELETE, "", null, ADMIN);

        assertThat(hidden.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(code(hidden)).isEqualTo("PROBLEM_IN_ACTIVE_CONTEST");
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ContestServiceStub.failWith(503);
        assertThat(send(HttpMethod.POST, "/hide", null, ADMIN).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(column("visibility")).isEqualTo("CONTEST_ONLY");
    }

    @Test
    @DisplayName("Xoá mềm bài đã xuất bản thì bài biến khỏi kho")
    void delete_softDeletesProblem() {
        assertThat(send(HttpMethod.DELETE, "", null, ADMIN).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(column("deleted_at")).isNotNull();
        assertThat(send(HttpMethod.POST, "/restore", null, ADMIN).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Chuyển chủ sở hữu chỉ cho người ra đề đang hoạt động")
    void transferOwnership_requiresActiveSetter() {
        AuthServiceStub.account(NEW_OWNER_ID, "an.bui", "SETTER", true);
        AuthServiceStub.account(UUID.randomUUID(), "hoc.vien", "LEARNER", true);
        AuthServiceStub.account(UUID.randomUUID(), "bi.khoa", "SETTER", false);

        assertThat(code(transfer("hoc.vien"))).isEqualTo("NEW_OWNER_NOT_SETTER");
        assertThat(code(transfer("bi.khoa"))).isEqualTo("NEW_OWNER_NOT_SETTER");
        assertThat(transfer("khong.ton.tai").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(transfer("an.bui").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT BIN_TO_UUID(author_id) FROM problems WHERE id ="
                                        + " UUID_TO_BIN(?)",
                                String.class,
                                problemId.toString()))
                .isEqualTo(NEW_OWNER_ID.toString());
    }

    @Test
    @DisplayName("Không phải ADMIN nhận 403, bài không tồn tại trả 404")
    void actions_requireAdminAndExistingProblem() {
        assertThat(send(HttpMethod.POST, "/hide", null, SETTER).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.DELETE, "", null, SETTER).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        problemId = UUID.randomUUID();
        assertThat(send(HttpMethod.POST, "/hide", null, ADMIN).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<String> transfer(String username) {
        return send(
                HttpMethod.POST,
                "/transfer-ownership",
                "{\"username\": \"" + username + "\"}",
                ADMIN);
    }

    private ResponseEntity<String> send(
            HttpMethod method, String suffix, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                "/api/admin/problems/" + problemId + suffix,
                method,
                new HttpEntity<>(body, headers),
                String.class);
    }

    private String column(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT " + name + " FROM problems WHERE id = UUID_TO_BIN(?)",
                String.class,
                problemId.toString());
    }

    private static String code(ResponseEntity<String> response) {
        return JsonPath.parse(response.getBody()).read("$.code", String.class);
    }

    private void tag(String slug, String name) {
        Tag tag = new Tag();
        tag.setSlug(slug);
        tag.setName(name);
        tagRepository.save(tag);
    }
}
