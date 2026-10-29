package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.LogicalType;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ReferenceSolution;
import com.sqljudge.problem.entity.RevisionColumn;
import com.sqljudge.problem.entity.RevisionTable;
import com.sqljudge.problem.entity.StatementType;
import com.sqljudge.problem.entity.Tag;
import com.sqljudge.problem.entity.Testcase;
import com.sqljudge.problem.entity.TestcaseData;
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

    @Autowired private EntityManager entityManager;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Lưu bài đã xuất bản cùng revision, schema, testcase, đáp án và đọc lại đủ")
    void save_publishedProblemWithRevision_roundTripsWholeGraph() {
        Problem problem = ProblemFixtures.publishedProblem("highest-salary-per-department", 184);
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
        assertThat(revision.getTables())
                .extracting(RevisionTable::getName)
                .containsExactly("departments", "employees");
        assertThat(revision.getTables().get(1).getColumns())
                .extracting(RevisionColumn::getName, RevisionColumn::getLogicalType)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("id", LogicalType.INTEGER),
                        org.assertj.core.groups.Tuple.tuple("department_id", LogicalType.INTEGER));
        assertThat(revision.getReferenceSolutions())
                .extracting(ReferenceSolution::getDialect)
                .containsExactlyInAnyOrder(Dialect.POSTGRESQL, Dialect.MYSQL);
        Testcase example = revision.getTestcases().get(0);
        assertThat(example.isPublic()).isTrue();
        assertThat(example.getData()).extracting(TestcaseData::getRowCount).containsExactly(2, 3);
        assertThat(example.getExpectedResult().getRowCount()).isEqualTo(2);
        assertThat(revision.getTestcases().get(1).isPublic()).isFalse();
    }

    @Test
    @DisplayName("Trùng slug hoặc trùng số bài thì DB chặn bằng unique index")
    void save_duplicateSlugOrNumber_isRejectedByUniqueIndex() {
        problemRepository.saveAndFlush(ProblemFixtures.publishedProblem("trung-slug", 1));

        assertThatThrownBy(
                        () ->
                                problemRepository.saveAndFlush(
                                        ProblemFixtures.publishedProblem("trung-slug", 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
        entityManager.clear();
        assertThatThrownBy(
                        () ->
                                problemRepository.saveAndFlush(
                                        ProblemFixtures.publishedProblem("slug-khac", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Mỗi revision chỉ có một đáp án chuẩn cho mỗi dialect")
    void save_twoSolutionsForSameDialect_isRejected() {
        Problem problem = ProblemFixtures.publishedProblem("hai-dap-an", 3);
        problem.getPublishedRevision()
                .addReferenceSolution(new ReferenceSolution(Dialect.POSTGRESQL, "SELECT 2;"));

        assertThatThrownBy(() -> problemRepository.saveAndFlush(problem))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Bỏ testcase khỏi revision thì xoá luôn dữ liệu và kết quả mong đợi của nó")
    void removeTestcase_deletesItsDataAndExpectedResult() {
        Problem problem =
                problemRepository.saveAndFlush(ProblemFixtures.publishedProblem("xoa-testcase", 4));

        problem.getPublishedRevision().getTestcases().remove(0);
        problemRepository.saveAndFlush(problem);

        assertThat(count("testcases")).isEqualTo(1);
        assertThat(count("testcase_data")).isEqualTo(2);
        assertThat(count("expected_results")).isEqualTo(1);
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
