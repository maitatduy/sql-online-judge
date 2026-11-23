package com.sqljudge.contest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.contest.service.ContestStatementWarmer;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class ContestStatementWarmupIntegrationTest extends ContainerSupport {

    private static final UUID FIRST_PROBLEM = UUID.randomUUID();
    private static final UUID SECOND_PROBLEM = UUID.randomUUID();

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ContestStatementWarmer statementWarmer;

    @DynamicPropertySource
    static void problemService(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.problem-service-url", ProblemServiceStub::url);
    }

    @BeforeEach
    void clear() {
        ProblemServiceStub.reset();
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM contest_rating_changes");
        jdbcTemplate.update("DELETE FROM contest_standings");
        jdbcTemplate.update("DELETE FROM contest_cells");
        jdbcTemplate.update("DELETE FROM contest_attempts");
        jdbcTemplate.update("DELETE FROM contest_registrations");
        jdbcTemplate.update("DELETE FROM contest_problems");
        jdbcTemplate.update("DELETE FROM contests");
    }

    @Test
    @DisplayName("Contest bắt đầu trong vòng 1 phút thì tải sẵn đề mỗi bài đúng một lần")
    void warmDue_contestStartingSoon_fetchesEachStatementOnce() {
        ProblemServiceStub.statement(FIRST_PROBLEM, "Bài A");
        ProblemServiceStub.statement(SECOND_PROBLEM, "Bài B");
        insertContest("sql-cup", Duration.ofSeconds(30), FIRST_PROBLEM, SECOND_PROBLEM);

        statementWarmer.warmDue();
        statementWarmer.warmDue();

        assertThat(ProblemServiceStub.statementRequests(FIRST_PROBLEM)).isEqualTo(1);
        assertThat(ProblemServiceStub.statementRequests(SECOND_PROBLEM)).isEqualTo(1);
        assertThat(warmedAt("sql-cup")).isNotNull();
    }

    @Test
    @DisplayName("Contest còn xa giờ bắt đầu thì chưa tải đề")
    void warmDue_contestFarAway_fetchesNothing() {
        ProblemServiceStub.statement(FIRST_PROBLEM, "Bài A");
        insertContest("sql-cup-xa", Duration.ofHours(2), FIRST_PROBLEM);

        statementWarmer.warmDue();

        assertThat(ProblemServiceStub.statementRequests(FIRST_PROBLEM)).isZero();
        assertThat(warmedAt("sql-cup-xa")).isNull();
    }

    @Test
    @DisplayName("Có bài lấy đề lỗi thì chưa đánh dấu, lần sau thử lại")
    void warmDue_statementFails_retriesNextTime() {
        ProblemServiceStub.statement(FIRST_PROBLEM, "Bài A");
        insertContest("sql-cup-loi", Duration.ofSeconds(30), FIRST_PROBLEM, SECOND_PROBLEM);

        statementWarmer.warmDue();
        assertThat(warmedAt("sql-cup-loi")).isNull();

        ProblemServiceStub.statement(SECOND_PROBLEM, "Bài B");
        statementWarmer.warmDue();

        assertThat(ProblemServiceStub.statementRequests(SECOND_PROBLEM)).isEqualTo(2);
        assertThat(warmedAt("sql-cup-loi")).isNotNull();
    }

    private void insertContest(String slug, Duration startsIn, UUID... problemIds) {
        jdbcTemplate.update(
                "INSERT INTO contests (id, slug, title, description, start_at, duration_minutes,"
                        + " end_at, freeze_minutes, penalty_minutes, is_rated, version, created_at,"
                        + " updated_at) VALUES (UUID_TO_BIN(UUID()), ?, 'SQL Cup', '',"
                        + " DATE_ADD(UTC_TIMESTAMP(6), INTERVAL ? SECOND), 90,"
                        + " DATE_ADD(UTC_TIMESTAMP(6), INTERVAL ? SECOND), 30, 20, false, 0,"
                        + " NOW(6), NOW(6))",
                slug,
                startsIn.toSeconds(),
                startsIn.plusMinutes(90).toSeconds());
        for (int position = 0; position < problemIds.length; position++) {
            jdbcTemplate.update(
                    "INSERT INTO contest_problems (id, contest_id, problem_id, position, slug,"
                            + " title, difficulty, created_at, updated_at) SELECT"
                            + " UUID_TO_BIN(UUID()), id, UUID_TO_BIN(?), ?, ?, 'Bài', 'EASY',"
                            + " NOW(6), NOW(6) FROM contests WHERE slug = ?",
                    problemIds[position].toString(),
                    position,
                    "bai-" + position,
                    slug);
        }
    }

    private Object warmedAt(String slug) {
        return jdbcTemplate.queryForObject(
                "SELECT statements_warmed_at FROM contests WHERE slug = ?", Object.class, slug);
    }
}
