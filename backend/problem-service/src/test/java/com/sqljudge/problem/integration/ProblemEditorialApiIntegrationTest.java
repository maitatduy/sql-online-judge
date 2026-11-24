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
class ProblemEditorialApiIntegrationTest extends ContainerSupport {

    private static final String SLUG = "highest-salary-per-department";
    private static final String EDITORIAL = "Dùng `RANK() OVER (PARTITION BY department_id)`.";
    private static final String PATH = "/api/problems/" + SLUG + "/editorial";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private DatasetRepository datasetRepository;
    @Autowired private UserProblemProgressRepository progressRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteProblems() {
        jdbcTemplate.update("DELETE FROM user_problem_progress");
        jdbcTemplate.update("UPDATE problems SET published_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Chỉ người đã AC, kiểm duyệt viên mới đọc được lời giải; người khác thấy LOCKED")
    void editorial_visibleOnlyAfterAccepted() {
        Problem problem = seed(EDITORIAL);
        UUID solver = UUID.randomUUID();
        UUID attempter = UUID.randomUUID();
        progressRepository.save(
                new UserProblemProgress(solver, problem.getId(), ProblemProgressStatus.SOLVED));
        progressRepository.save(
                new UserProblemProgress(
                        attempter, problem.getId(), ProblemProgressStatus.ATTEMPTED));

        assertLocked(getOk(null));
        assertLocked(getOk(TestJwts.token(attempter, "LEARNER")));
        DocumentContext solved = getOk(TestJwts.token(solver, "LEARNER"));
        assertThat(solved.read("$.status", String.class)).isEqualTo("AVAILABLE");
        assertThat(solved.read("$.editorialMarkdown", String.class)).isEqualTo(EDITORIAL);
        assertThat(
                        getOk(TestJwts.token(UUID.randomUUID(), "MODERATOR"))
                                .read("$.status", String.class))
                .isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("Bài chưa có lời giải thì NONE, bài không tồn tại thì 404")
    void editorial_missing() {
        seed(null);

        assertThat(getOk(null).read("$.status", String.class)).isEqualTo("NONE");
        assertThat(get("/api/problems/khong-co/editorial", null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Problem seed(String editorial) {
        Dataset dataset = datasetRepository.save(ProblemFixtures.companyDataset("cong-ty"));
        Problem problem = ProblemFixtures.publishedProblem(SLUG, 184, dataset);
        problem.setEditorialMarkdown(editorial);
        return problemRepository.save(problem);
    }

    private static void assertLocked(DocumentContext editorial) {
        assertThat(editorial.read("$.status", String.class)).isEqualTo("LOCKED");
        assertThat(editorial.read("$.editorialMarkdown", String.class)).isNull();
    }

    private DocumentContext getOk(String token) {
        ResponseEntity<String> response = get(PATH, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody());
    }

    private ResponseEntity<String> get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
