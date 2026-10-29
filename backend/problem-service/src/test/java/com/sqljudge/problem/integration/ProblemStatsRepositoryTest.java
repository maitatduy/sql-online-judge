package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.common.config.CommonAutoConfiguration;
import com.sqljudge.problem.entity.ProblemProgressStatus;
import com.sqljudge.problem.entity.ProblemStats;
import com.sqljudge.problem.entity.UserProblemProgress;
import com.sqljudge.problem.repository.ProblemStatsRepository;
import com.sqljudge.problem.repository.UserProblemProgressRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class ProblemStatsRepositoryTest extends ContainerSupport {

    private static final UUID PROBLEM_ID = UUID.fromString("0192f3b0-0000-7000-8000-0000000000b8");
    private static final UUID USER_ID = UUID.fromString("0192f3a0-0000-7000-8000-000000000001");

    @Autowired private ProblemStatsRepository problemStatsRepository;

    @Autowired private UserProblemProgressRepository userProblemProgressRepository;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void insertProblem() {
        jdbcTemplate.update(
                "INSERT INTO problems (id, slug, title, difficulty, status, author_id,"
                        + " statement_markdown, starter_code, time_limit_ms, requires_order,"
                        + " compare_column_names, float_tolerance, allowed_statements, version,"
                        + " created_at, updated_at) VALUES (UUID_TO_BIN(?), 'bai-mau', 'Bài mẫu',"
                        + " 'EASY', 'DRAFT', UUID_TO_BIN(?), '', '', 2000, FALSE, FALSE, 0.000001,"
                        + " 'SELECT', 0, NOW(6), NOW(6))",
                PROBLEM_ID.toString(),
                USER_ID.toString());
    }

    @Test
    @DisplayName("Thống kê bài tính tỉ lệ AC theo phần trăm, làm tròn 1 chữ số")
    void save_stats_computesAcceptanceRate() {
        problemStatsRepository.saveAndFlush(new ProblemStats(PROBLEM_ID, 1000, 412, 12380));

        ProblemStats stats = problemStatsRepository.findByProblemId(PROBLEM_ID).orElseThrow();

        assertThat(stats.acceptanceRate()).isEqualTo(41.2);
        assertThat(stats.getSolvedUserCount()).isEqualTo(12380);
        assertThat(new ProblemStats(PROBLEM_ID, 0, 0, 0).acceptanceRate()).isZero();
    }

    @Test
    @DisplayName("Mỗi bài chỉ có một dòng thống kê")
    void save_secondStatsForSameProblem_isRejected() {
        problemStatsRepository.saveAndFlush(new ProblemStats(PROBLEM_ID, 1, 1, 1));

        assertThatThrownBy(
                        () ->
                                problemStatsRepository.saveAndFlush(
                                        new ProblemStats(PROBLEM_ID, 2, 2, 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Mỗi người chỉ có một dòng tiến độ cho mỗi bài")
    void save_secondProgressForSameUserAndProblem_isRejected() {
        userProblemProgressRepository.saveAndFlush(
                new UserProblemProgress(USER_ID, PROBLEM_ID, ProblemProgressStatus.ATTEMPTED));

        assertThatThrownBy(
                        () ->
                                userProblemProgressRepository.saveAndFlush(
                                        new UserProblemProgress(
                                                USER_ID, PROBLEM_ID, ProblemProgressStatus.SOLVED)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
