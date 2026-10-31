package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemStats;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.entity.Tag;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.ProblemStatsRepository;
import com.sqljudge.problem.repository.TagRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class ProblemDetailApiIntegrationTest extends ContainerSupport {

    private static final String SLUG = "highest-salary-per-department";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private ProblemRepository problemRepository;

    @Autowired private TagRepository tagRepository;

    @Autowired private ProblemStatsRepository problemStatsRepository;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedProblem() {
        Tag join = new Tag();
        join.setSlug("join");
        join.setName("JOIN");
        Problem problem = ProblemFixtures.publishedProblem(SLUG, 184);
        problem.getTags().add(tagRepository.save(join));
        problemRepository.save(problem);
        problemStatsRepository.save(new ProblemStats(problem.getId(), 1000, 412, 12380));
    }

    @AfterEach
    void deleteProblems() {
        jdbcTemplate.update("UPDATE problems SET published_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM tags");
    }

    @Test
    @DisplayName("Trả đề bài đúng dạng ProblemDetail: thông tin, đề, schema, ví dụ, ràng buộc")
    void detail_publishedProblem_returnsProblemDetail() {
        DocumentContext detail = getOk("/api/problems/" + SLUG);

        assertThat(detail.read("$.number", Integer.class)).isEqualTo(184);
        assertThat(detail.read("$.title", String.class)).isEqualTo("Lương cao nhất mỗi phòng ban");
        assertThat(detail.read("$.difficulty", String.class)).isEqualTo("MEDIUM");
        assertThat(detail.read("$.tags[0].slug", String.class)).isEqualTo("join");
        assertThat(detail.read("$.statementMarkdown", String.class)).contains("**lương cao nhất**");
        assertThat(detail.read("$.starterCode", String.class))
                .isEqualTo("SELECT *\nFROM employees;");
        assertThat(detail.read("$.dialects", List.class)).containsExactly("POSTGRESQL", "MYSQL");
        assertThat(detail.read("$.engineVersions", Map.class))
                .containsEntry("POSTGRESQL", "PostgreSQL 16")
                .containsEntry("MYSQL", "MySQL 8")
                .doesNotContainKey("SQLITE");
        assertThat(detail.read("$.schema[*].name", List.class))
                .containsExactly("departments", "employees");
        assertThat(detail.read("$.schema[1].columns[1]", Map.class))
                .containsEntry("name", "department_id")
                .containsEntry("type", "INTEGER")
                .containsEntry("isPrimaryKey", false)
                .containsEntry("isNullable", true)
                .containsEntry("references", Map.of("table", "departments", "column", "id"));
        assertThat(detail.read("$.schema[0].columns[0].references", Object.class)).isNull();
        assertThat(detail.read("$.requiresOrder", Boolean.class)).isTrue();
        assertThat(detail.read("$.allowedStatements", List.class))
                .containsExactly("SELECT", "WITH");
        assertThat(detail.read("$.timeLimitMs", Integer.class)).isEqualTo(2000);
        assertThat(detail.read("$.hiddenTestcaseCount", Integer.class)).isEqualTo(1);
        assertThat(detail.read("$.acceptanceRate", Double.class)).isEqualTo(41.2);
        assertThat(detail.read("$.solvedCount", Integer.class)).isEqualTo(12380);
    }

    @Test
    @DisplayName("Ví dụ chỉ lấy kết quả của testcase công khai, giữ nguyên NULL")
    void detail_examples_containOnlyPublicTestcaseResults() {
        DocumentContext detail = getOk("/api/problems/" + SLUG);

        assertThat(detail.read("$.examples.length()", Integer.class)).isEqualTo(1);
        assertThat(detail.read("$.examples[0].columns", List.class))
                .containsExactly("department", "employee");
        assertThat(detail.read("$.examples[0].rows[0]", List.class))
                .containsExactly("Kỹ thuật", "Hoàng Quốc Bảo");
        assertThat(detail.read("$.examples[0].rows[1][1]", Object.class)).isNull();
        assertThat(detail.read("$.examples[0].isTruncated", Boolean.class)).isFalse();
    }

    @Test
    @DisplayName("Không bao giờ trả dữ liệu testcase ẩn, đáp án chuẩn hay dữ liệu CSV")
    void detail_neverLeaksHiddenDataOrReferenceSolution() {
        String body = restTemplate.getForEntity("/api/problems/" + SLUG, String.class).getBody();

        assertThat(body)
                .doesNotContain(ProblemFixtures.HIDDEN_DATA_MARKER)
                .doesNotContain("dap-an-chuan")
                .doesNotContain("csv")
                .doesNotContain("referenceSolution")
                .doesNotContain("expectedResult")
                .doesNotContain("authorId");
    }

    @Test
    @DisplayName("Bài không tồn tại, đang ẩn hoặc chưa xuất bản đều trả 404 PROBLEM_NOT_FOUND")
    void detail_unknownHiddenOrDraftProblem_returnsNotFound() {
        Problem hidden = ProblemFixtures.publishedProblem("hidden-problem", 2);
        hidden.setVisibility(ProblemVisibility.HIDDEN);
        Problem draft = ProblemFixtures.publishedProblem("draft-problem", 3);
        draft.setStatus(ProblemStatus.DRAFT);
        problemRepository.saveAll(List.of(hidden, draft));

        for (String slug : List.of("khong-co-bai-nay", "hidden-problem", "draft-problem")) {
            ResponseEntity<String> response =
                    restTemplate.getForEntity("/api/problems/" + slug, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                    .isEqualTo("PROBLEM_NOT_FOUND");
        }
    }

    private DocumentContext getOk(String path) {
        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody());
    }
}
