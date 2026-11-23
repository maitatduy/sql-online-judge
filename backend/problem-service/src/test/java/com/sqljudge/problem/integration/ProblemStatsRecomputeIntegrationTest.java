package com.sqljudge.problem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.problem.client.JudgingSummary;
import com.sqljudge.problem.client.JudgingSummary.UserJudging;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemProgressStatus;
import com.sqljudge.problem.entity.ProblemStats;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.entity.UserProblemProgress;
import com.sqljudge.problem.event.RejudgeFinishedEvent;
import com.sqljudge.problem.event.SubmissionJudgedEvent;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.ProblemStatsRepository;
import com.sqljudge.problem.repository.UserProblemProgressRepository;
import com.sqljudge.problem.service.ProblemStatsRecomputeService;
import com.sqljudge.problem.service.SubmissionStatsService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProblemStatsRecomputeIntegrationTest extends ContainerSupport {

    private static final UUID WAS_SOLVED = UUID.randomUUID();
    private static final UUID NOW_SOLVED = UUID.randomUUID();
    private static final UUID NEWCOMER = UUID.randomUUID();
    private static final Instant SOLVED_AT = Instant.parse("2026-11-20T10:00:00Z");

    @Autowired private ProblemRepository problemRepository;
    @Autowired private ProblemStatsRepository statsRepository;
    @Autowired private UserProblemProgressRepository progressRepository;
    @Autowired private ProblemStatsRecomputeService recomputeService;
    @Autowired private SubmissionStatsService submissionStatsService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID problemId;

    @BeforeEach
    void seed() {
        Problem problem = new Problem();
        problem.setNumber(9);
        problem.setSlug("recompute");
        problem.setTitle("Bài tính lại");
        problem.setDifficulty(Difficulty.EASY);
        problem.setStatus(ProblemStatus.PUBLISHED);
        problem.setVisibility(ProblemVisibility.PUBLIC);
        problem.setAuthorId(UUID.randomUUID());
        problem.setStatementMarkdown("");
        problem.setStarterCode("");
        problem.setTimeLimitMs(2000);
        problemId = problemRepository.save(problem).getId();
        statsRepository.save(new ProblemStats(problemId, 100, 50, 2));
        progressRepository.saveAll(
                List.of(
                        new UserProblemProgress(
                                WAS_SOLVED, problemId, ProblemProgressStatus.SOLVED),
                        new UserProblemProgress(
                                NOW_SOLVED, problemId, ProblemProgressStatus.ATTEMPTED)));
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM processed_events");
        jdbcTemplate.update("DELETE FROM problems");
    }

    @Test
    @DisplayName(
            "Chấm lại xong thì thống kê và tiến độ được ghi đè theo kết quả mới, chạy lại không"
                    + " đổi")
    void replace_overwritesStatsAndProgressOnce() {
        RejudgeFinishedEvent event =
                new RejudgeFinishedEvent(
                        UUID.randomUUID(), Instant.now(), 1, problemId, UUID.randomUUID());
        JudgingSummary summary =
                new JudgingSummary(
                        100,
                        10,
                        List.of(
                                new UserJudging(WAS_SOLVED, null),
                                new UserJudging(NOW_SOLVED, SOLVED_AT),
                                new UserJudging(NEWCOMER, null)));

        recomputeService.replace(event, summary);
        recomputeService.replace(event, new JudgingSummary(1, 1, List.of()));

        ProblemStats stats = statsRepository.findByProblemId(problemId).orElseThrow();
        assertThat(stats.getSubmissionCount()).isEqualTo(100);
        assertThat(stats.getAcceptedCount()).isEqualTo(10);
        assertThat(stats.getSolvedUserCount()).isEqualTo(1);
        assertThat(progressOf(WAS_SOLVED).getStatus()).isEqualTo(ProblemProgressStatus.ATTEMPTED);
        assertThat(progressOf(WAS_SOLVED).getFirstSolvedAt()).isNull();
        assertThat(progressOf(NOW_SOLVED).getStatus()).isEqualTo(ProblemProgressStatus.SOLVED);
        assertThat(progressOf(NOW_SOLVED).getFirstSolvedAt()).isEqualTo(SOLVED_AT);
        assertThat(progressOf(NEWCOMER).getStatus()).isEqualTo(ProblemProgressStatus.ATTEMPTED);
        assertThat(recomputeService.isProcessed(event.eventId())).isTrue();
    }

    @Test
    @DisplayName("Event judged của lần chấm lại không cộng dồn vào thống kê")
    void rejudgedEvent_doesNotIncrementStats() {
        submissionStatsService.record(
                new SubmissionJudgedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        1,
                        UUID.randomUUID(),
                        NEWCOMER,
                        problemId,
                        "AC",
                        Instant.now(),
                        true));

        ProblemStats stats = statsRepository.findByProblemId(problemId).orElseThrow();
        assertThat(stats.getSubmissionCount()).isEqualTo(100);
        assertThat(progressRepository.findByUserIdAndProblemId(NEWCOMER, problemId)).isEmpty();
    }

    private UserProblemProgress progressOf(UUID userId) {
        return progressRepository.findByUserIdAndProblemId(userId, problemId).orElseThrow();
    }
}
