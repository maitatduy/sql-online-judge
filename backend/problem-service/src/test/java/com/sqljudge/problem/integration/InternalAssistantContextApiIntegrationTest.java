package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemProgressStatus;
import com.sqljudge.problem.entity.UserProblemProgress;
import com.sqljudge.problem.repository.DatasetRepository;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.UserProblemProgressRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InternalAssistantContextApiIntegrationTest extends ContainerSupport {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String INTERNAL_TOKEN = "test-internal-token";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private DatasetRepository datasetRepository;
    @Autowired private UserProblemProgressRepository progressRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Problem problem;

    @BeforeEach
    void seedProblem() {
        Dataset dataset = datasetRepository.save(ProblemFixtures.companyDataset("cong-ty"));
        problem =
                problemRepository.save(
                        ProblemFixtures.publishedProblem("highest-salary", 184, dataset));
    }

    @AfterEach
    void deleteProblems() {
        jdbcTemplate.update("DELETE FROM user_problem_progress");
        jdbcTemplate.update("UPDATE problems SET published_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Trả đề, schema, trạng thái AC; không lộ dữ liệu ẩn hay đáp án chuẩn")
    void context_returnsPublicPartsAndSolvedFlag() {
        UUID solver = UUID.randomUUID();
        progressRepository.save(
                new UserProblemProgress(solver, problem.getId(), ProblemProgressStatus.SOLVED));

        ResponseEntity<String> solved = context(problem.getId(), solver, INTERNAL_TOKEN);
        ResponseEntity<String> fresh = context(problem.getId(), UUID.randomUUID(), INTERNAL_TOKEN);

        assertThat(solved.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext body = JsonPath.parse(solved.getBody());
        assertThat(body.read("$.title", String.class)).isEqualTo("Lương cao nhất mỗi phòng ban");
        assertThat(body.read("$.schema[*].name", List.class))
                .containsExactly("departments", "employees");
        assertThat(body.read("$.isSolved", Boolean.class)).isTrue();
        assertThat(JsonPath.parse(fresh.getBody()).read("$.isSolved", Boolean.class)).isFalse();
        assertThat(solved.getBody())
                .doesNotContain(ProblemFixtures.HIDDEN_DATA_MARKER)
                .doesNotContain(ProblemFixtures.REFERENCE_SOLUTION_MARKER);
    }

    @Test
    @DisplayName("Thiếu token nội bộ thì 401, bài không tồn tại thì 404")
    void context_guarded() {
        assertThat(context(problem.getId(), UUID.randomUUID(), null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(context(UUID.randomUUID(), UUID.randomUUID(), INTERNAL_TOKEN).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<String> context(UUID problemId, UUID userId, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set(INTERNAL_TOKEN_HEADER, token);
        }
        return restTemplate.exchange(
                "/internal/problems/" + problemId + "/assistant-context?userId=" + userId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }
}
