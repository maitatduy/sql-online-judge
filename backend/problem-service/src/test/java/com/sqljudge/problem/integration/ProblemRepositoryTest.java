package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.problem.entity.DatasetTable;
import com.sqljudge.problem.entity.DatasetVariant;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ReferenceSolution;
import com.sqljudge.problem.entity.StatementType;
import com.sqljudge.problem.entity.Tag;
import com.sqljudge.problem.entity.Testcase;
import com.sqljudge.problem.repository.DatasetRepository;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.TagRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(CommonAutoConfiguration.class)
class ProblemRepositoryTest extends ContainerSupport {

    private static final UUID AUTHOR_ID = UUID.fromString("0192f3a0-0000-7000-8000-000000000001");

    @Autowired private ProblemRepository problemRepository;

    @Autowired private TagRepository tagRepository;

    @Autowired private DatasetRepository datasetRepository;

    @Autowired private EntityManager entityManager;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Lưu bài đã xuất bản cùng revision, dataset, testcase, đáp án và đọc lại đủ")
    void save_publishedProblemWithRevision_roundTripsWholeGraph() {
        Problem problem = problem("highest-salary-per-department", 184);
        problem.getTags().add(tag("join", "JOIN"));
        problemRepository.saveAndFlush(problem);
        entityManager.clear();

        Problem loaded =
                problemRepository.findBySlug("highest-salary-per-department").orElseThrow();

        assertThat(loaded.getId().version()).isEqualTo(7);
        assertThat(loaded.getAllowedStatements())
                .containsExactlyInAnyOrder(StatementType.SELECT, StatementType.WITH);
        assertThat(loaded.getTags()).extracting(Tag::getSlug).containsExactly("join");
        ProblemRevision revision = loaded.getPublishedRevision();
        assertThat(revision.getFrozenAt()).isNotNull();
        assertThat(revision.getDatasetVersion().getTables())
                .extracting(DatasetTable::getName)
                .containsExactly("departments", "employees");
        assertThat(revision.getReferenceSolutions())
                .extracting(ReferenceSolution::getDialect)
                .containsExactlyInAnyOrder(Dialect.POSTGRESQL, Dialect.MYSQL);
        Testcase example = revision.getTestcases().get(0);
        assertThat(example.isPublic()).isTrue();
        assertThat(example.getDatasetVariant())
                .extracting(DatasetVariant::getName)
                .isEqualTo("Mẫu");
        assertThat(example.getExpectedResult().getRowCount()).isEqualTo(2);
        assertThat(revision.getTestcases().get(1).isPublic()).isFalse();
    }

    @Test
    @DisplayName("Trùng slug hoặc trùng số bài thì DB chặn bằng unique index")
    void save_duplicateSlugOrNumber_isRejectedByUniqueIndex() {
        problemRepository.saveAndFlush(problem("trung-slug", 1));

        assertThatThrownBy(() -> problemRepository.saveAndFlush(problem("trung-slug", 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
        entityManager.clear();
        assertThatThrownBy(() -> problemRepository.saveAndFlush(problem("slug-khac", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Mỗi revision chỉ có một đáp án chuẩn cho mỗi dialect")
    void save_twoSolutionsForSameDialect_isRejected() {
        Problem problem = problem("hai-dap-an", 3);
        problem.getPublishedRevision()
                .addReferenceSolution(new ReferenceSolution(Dialect.POSTGRESQL, "SELECT 2;"));

        assertThatThrownBy(() -> problemRepository.saveAndFlush(problem))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Bỏ testcase khỏi revision thì xoá kết quả mong đợi, giữ nguyên bộ dữ liệu")
    void removeTestcase_deletesExpectedResultButKeepsVariant() {
        Problem problem = problemRepository.saveAndFlush(problem("xoa-testcase", 4));

        problem.getPublishedRevision().getTestcases().remove(0);
        problemRepository.saveAndFlush(problem);

        assertThat(count("testcases")).isEqualTo(1);
        assertThat(count("expected_results")).isEqualTo(1);
        assertThat(count("dataset_variants")).isEqualTo(2);
    }

    private Problem problem(String slug, int number) {
        return ProblemFixtures.publishedProblem(
                slug,
                number,
                datasetRepository.save(ProblemFixtures.companyDataset(slug + "-" + number)));
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private Tag tag(String slug, String name) {
        Tag tag = new Tag();
        tag.setSlug(slug);
        tag.setName(name);
        return tagRepository.save(tag);
    }
}
