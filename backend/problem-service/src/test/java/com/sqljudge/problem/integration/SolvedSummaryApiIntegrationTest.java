package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.entity.Difficulty;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SolvedSummaryApiIntegrationTest extends ContainerSupport {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private DatasetRepository datasetRepository;
    @Autowired private UserProblemProgressRepository progressRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM user_problem_progress");
        jdbcTemplate.update("UPDATE problems SET published_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName(
            "Đếm bài đã giải theo độ khó trên tổng bài công khai; bài chỉ thử chưa giải không tính")
    void summary_countsSolvedByDifficulty() {
        Dataset dataset = datasetRepository.save(ProblemFixtures.companyDataset("cong-ty"));
        Problem easy = publish("bai-de", 1, Difficulty.EASY, dataset);
        publish("bai-de-2", 2, Difficulty.EASY, dataset);
        Problem medium = publish("bai-vua", 3, Difficulty.MEDIUM, dataset);
        Problem hard = publish("bai-kho", 4, Difficulty.HARD, dataset);
        UUID userId = UUID.randomUUID();
        progress(userId, easy, ProblemProgressStatus.SOLVED);
        progress(userId, medium, ProblemProgressStatus.SOLVED);
        progress(userId, hard, ProblemProgressStatus.ATTEMPTED);

        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/solved-summaries/" + userId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext summary = JsonPath.parse(response.getBody());
        assertThat(summary.read("$.solvedCount", Integer.class)).isEqualTo(2);
        assertThat(summary.read("$.byDifficulty.EASY.solved", Integer.class)).isEqualTo(1);
        assertThat(summary.read("$.byDifficulty.EASY.total", Integer.class)).isEqualTo(2);
        assertThat(summary.read("$.byDifficulty.MEDIUM.solved", Integer.class)).isEqualTo(1);
        assertThat(summary.read("$.byDifficulty.HARD.solved", Integer.class)).isZero();
        assertThat(summary.read("$.byDifficulty.HARD.total", Integer.class)).isEqualTo(1);
    }

    private Problem publish(String slug, int number, Difficulty difficulty, Dataset dataset) {
        Problem problem = ProblemFixtures.publishedProblem(slug, number, dataset);
        problem.setDifficulty(difficulty);
        return problemRepository.save(problem);
    }

    private void progress(UUID userId, Problem problem, ProblemProgressStatus status) {
        progressRepository.save(new UserProblemProgress(userId, problem.getId(), status));
    }
}
