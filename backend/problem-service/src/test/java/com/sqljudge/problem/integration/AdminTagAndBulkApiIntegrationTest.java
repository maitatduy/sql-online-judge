package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.entity.Tag;
import com.sqljudge.problem.repository.ProblemRepository;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class AdminTagAndBulkApiIntegrationTest extends ContainerSupport {

    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID firstId;
    private UUID secondId;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.contest-service-url", ContestServiceStub::url);
    }

    @BeforeEach
    void seed() {
        ContestServiceStub.reset();
        Tag join = tag("join", "JOIN");
        tag("cte", "CTE");
        Tag orphan = tag("orphan", "Mồ côi");
        firstId = problemRepository.save(published(1, "first", join)).getId();
        secondId = problemRepository.save(published(2, "second", join)).getId();
        Problem deleted = published(3, "deleted", orphan);
        deleted.setDeletedAt(Instant.parse("2026-11-01T00:00:00Z"));
        problemRepository.save(deleted);
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM tags");
    }

    @Test
    @DisplayName("Danh sách tag đếm bài chưa xoá đang dùng; không phải ADMIN nhận 403")
    void tags_countLiveProblems() {
        DocumentContext tags =
                JsonPath.parse(send(HttpMethod.GET, "/api/admin/tags", null, ADMIN).getBody());

        assertThat(tags.read("$[?(@.slug == 'join')].problemCount", List.class)).containsExactly(2);
        assertThat(tags.read("$[?(@.slug == 'orphan')].problemCount", List.class))
                .containsExactly(0);
        assertThat(send(HttpMethod.GET, "/api/admin/tags", null, SETTER).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Thao tác hàng loạt áp từng bài, bài lỗi được liệt kê kèm mã lỗi")
    void bulkUpdate_reportsEachProblem() {
        UUID missing = UUID.randomUUID();
        DocumentContext result =
                JsonPath.parse(
                        bulk(
                                        "[\"" + firstId + "\", \"" + secondId + "\", \"" + missing
                                                + "\"]",
                                        "\"addTagSlugs\": [\"cte\"], \"difficulty\": \"HARD\"")
                                .getBody());

        assertThat(result.read("$.succeeded", List.class))
                .containsExactlyInAnyOrder(firstId.toString(), secondId.toString());
        assertThat(result.read("$.failed[0].problemId", String.class))
                .isEqualTo(missing.toString());
        assertThat(result.read("$.failed[0].code", String.class)).isEqualTo("PROBLEM_NOT_FOUND");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM problems WHERE difficulty = 'HARD'",
                                Integer.class))
                .isEqualTo(2);
    }

    @Test
    @DisplayName(
            "Ẩn hàng loạt bỏ qua bài đang trong contest; thiếu thao tác trả 400, không phải ADMIN"
                    + " trả 403")
    void bulkHide_skipsProblemsInContest() {
        ContestServiceStub.inUnfinishedContest(secondId);

        DocumentContext result =
                JsonPath.parse(
                        bulk("[\"" + firstId + "\", \"" + secondId + "\"]", "\"hide\": true")
                                .getBody());

        assertThat(result.read("$.succeeded", List.class)).containsExactly(firstId.toString());
        assertThat(result.read("$.failed[0].code", String.class))
                .isEqualTo("PROBLEM_IN_ACTIVE_CONTEST");
        assertThat(bulk("[\"" + firstId + "\"]", "\"hide\": false").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(
                        send(
                                        HttpMethod.POST,
                                        "/api/admin/problems/bulk-update",
                                        "{\"problemIds\": [\"" + firstId + "\"], \"hide\": true}",
                                        SETTER)
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<String> bulk(String ids, String changes) {
        return send(
                HttpMethod.POST,
                "/api/admin/problems/bulk-update",
                "{\"problemIds\": " + ids + ", " + changes + "}",
                ADMIN);
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private static String code(ResponseEntity<String> response) {
        return JsonPath.parse(response.getBody()).read("$.code", String.class);
    }

    private Tag tag(String slug, String name) {
        Tag tag = new Tag();
        tag.setSlug(slug);
        tag.setName(name);
        return tagRepository.save(tag);
    }

    private static Problem published(int number, String slug, Tag tag) {
        Problem problem = new Problem();
        problem.setNumber(number);
        problem.setSlug(slug);
        problem.setTitle("Bài " + number);
        problem.setDifficulty(Difficulty.EASY);
        problem.setStatus(ProblemStatus.PUBLISHED);
        problem.setVisibility(ProblemVisibility.PUBLIC);
        problem.setAuthorId(UUID.randomUUID());
        problem.setStatementMarkdown("");
        problem.setStarterCode("");
        problem.setTimeLimitMs(2000);
        problem.getTags().add(tag);
        return problem;
    }
}
