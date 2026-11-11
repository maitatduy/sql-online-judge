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

@Entity
@Table(
        name = "submissions",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_submissions_user_id_idempotency_key",
                        columnNames = {"user_id", "idempotency_key"}),
        indexes =
                @Index(
                        name = "idx_submissions_user_id_problem_id_created_at",
                        columnList = "user_id, problem_id, created_at"))
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

    @Column(name = "judged_testcases", nullable = false)
    private int judgedTestcases;

    @Column(name = "total_testcases", nullable = false)
    private int totalTestcases;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

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
        submission.problemRevisionId = solution.revisionId();
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

    public JudgeOutcome complete(JudgeResult result, Instant now) {
        if (!isCurrent(result.attempt())) {
            return JudgeOutcome.IGNORED;
        }
        if (result.verdict() == Verdict.SE && attempts < MAX_ATTEMPTS) {
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
        judgedTestcases = result.judgedTestcases();
        judgedAt = now;
        return JudgeOutcome.COMPLETED;
    }

    private boolean isCurrent(int attempt) {
        return status != SubmissionStatus.COMPLETED && attempt == attempts;
    }

    public record SubmittedSolution(
            UUID problemId, UUID revisionId, Dialect dialect, String code, int testcaseCount) {}
}
