package com.sqljudge.submission.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "submissions",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_submissions_user_id_idempotency_key",
                        columnNames = {"user_id", "idempotency_key"}),
        indexes = {
            @Index(
                    name = "idx_submissions_user_id_problem_id_id",
                    columnList = "user_id, problem_id, id"),
            @Index(
                    name = "idx_submissions_problem_id_contest_id",
                    columnList = "problem_id, contest_id")
        })
@Getter
@NoArgsConstructor
public class Submission extends BaseEntity {

    private static final int FIRST_ATTEMPT = 1;
    private static final int MAX_ATTEMPTS = 3;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "problem_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID problemId;

    @Column(name = "problem_revision_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID problemRevisionId;

    @Column(name = "problem_number")
    private Integer problemNumber;

    @Column(name = "problem_slug", length = 160)
    private String problemSlug;

    @Column(name = "problem_title", length = 200)
    private String problemTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "dialect", nullable = false, length = 16)
    private Dialect dialect;

    @Column(name = "code", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SubmissionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", length = 8)
    private Verdict verdict;

    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "first_failed_index")
    private Integer firstFailedIndex;

    @Column(name = "first_failed_is_hidden")
    private Boolean firstFailedIsHidden;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diff_json", columnDefinition = "JSON")
    private ResultDiff diff;

    @Column(name = "judged_testcases", nullable = false)
    private int judgedTestcases;

    @Column(name = "total_testcases", nullable = false)
    private int totalTestcases;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "contest_id", columnDefinition = "BINARY(16)")
    private UUID contestId;

    @Column(name = "rejudge_batch_id", columnDefinition = "BINARY(16)")
    private UUID rejudgeBatchId;

    @Column(name = "round_start_attempt", nullable = false)
    private int roundStartAttempt = FIRST_ATTEMPT;

    @Column(name = "judged_at")
    private Instant judgedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public static Submission pending(
            UUID userId, SubmittedSolution solution, String idempotencyKey) {
        Submission submission = new Submission();
        submission.userId = userId;
        submission.problemId = solution.problemId();
        submission.contestId = solution.contestId();
        submission.problemRevisionId = solution.revisionId();
        submission.problemNumber = solution.problem().number();
        submission.problemSlug = solution.problem().slug();
        submission.problemTitle = solution.problem().title();
        submission.dialect = solution.dialect();
        submission.code = solution.code();
        submission.totalTestcases = solution.testcaseCount();
        submission.status = SubmissionStatus.PENDING;
        submission.attempts = FIRST_ATTEMPT;
        submission.idempotencyKey = idempotencyKey;
        return submission;
    }

    public boolean recordProgress(int attempt, int judged) {
        if (!isCurrent(attempt)) {
            return false;
        }
        status = SubmissionStatus.JUDGING;
        judgedTestcases = Math.max(judgedTestcases, judged);
        return true;
    }

    public boolean isInContest() {
        return contestId != null;
    }

    public JudgeOutcome complete(JudgeResult result, Instant now) {
        if (!isCurrent(result.attempt())) {
            return JudgeOutcome.IGNORED;
        }
        if (result.verdict() == Verdict.SE && attempts - roundStartAttempt + 1 < MAX_ATTEMPTS) {
            attempts++;
            status = SubmissionStatus.PENDING;
            judgedTestcases = 0;
            return JudgeOutcome.RETRY;
        }
        status = SubmissionStatus.COMPLETED;
        verdict = result.verdict();
        runtimeMs = (int) Math.min(Integer.MAX_VALUE, result.runtimeMs());
        errorMessage = result.message();
        firstFailedIndex = result.failedIndex();
        firstFailedIsHidden = result.failedIsHidden();
        diff =
                !isInContest() && Boolean.FALSE.equals(result.failedIsHidden())
                        ? result.diff()
                        : null;
        judgedTestcases = result.judgedTestcases();
        judgedAt = now;
        return JudgeOutcome.COMPLETED;
    }

    public void rejudge(UUID revisionId, int testcaseCount, UUID batchId) {
        attempts++;
        roundStartAttempt = attempts;
        problemRevisionId = revisionId;
        totalTestcases = testcaseCount;
        rejudgeBatchId = batchId;
        status = SubmissionStatus.PENDING;
        verdict = null;
        runtimeMs = null;
        errorMessage = null;
        firstFailedIndex = null;
        firstFailedIsHidden = null;
        diff = null;
        judgedTestcases = 0;
        judgedAt = null;
    }

    public UUID finishRejudge() {
        UUID batchId = rejudgeBatchId;
        rejudgeBatchId = null;
        return batchId;
    }

    private boolean isCurrent(int attempt) {
        return status != SubmissionStatus.COMPLETED && attempt == attempts;
    }

    public record SubmittedSolution(
            UUID problemId,
            UUID revisionId,
            ProblemLabel problem,
            Dialect dialect,
            String code,
            int testcaseCount,
            UUID contestId) {}

    public record ProblemLabel(Integer number, String slug, String title) {}
}
