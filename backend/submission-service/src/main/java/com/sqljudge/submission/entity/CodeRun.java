package com.sqljudge.submission.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "code_runs")
@Getter
@NoArgsConstructor
public class CodeRun extends BaseEntity {

    private static final int MAX_MESSAGE_LENGTH = 2000;

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
    private RunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 8)
    private RunOutcome outcome;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", columnDefinition = "JSON")
    private RunResult result;

    @Column(name = "error_message", length = MAX_MESSAGE_LENGTH)
    private String errorMessage;

    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public static CodeRun pending(
            UUID userId, UUID problemId, UUID revisionId, Dialect dialect, String code) {
        CodeRun run = new CodeRun();
        run.userId = userId;
        run.problemId = problemId;
        run.problemRevisionId = revisionId;
        run.dialect = dialect;
        run.code = code;
        run.status = RunStatus.PENDING;
        return run;
    }

    public boolean complete(RunCompletion completion, Instant now) {
        if (status == RunStatus.COMPLETED) {
            return false;
        }
        status = RunStatus.COMPLETED;
        outcome = completion.outcome();
        result = completion.outcome() == RunOutcome.OK ? completion.result() : null;
        errorMessage = shortened(completion.message());
        runtimeMs = (int) Math.min(Integer.MAX_VALUE, completion.runtimeMs());
        completedAt = now;
        return true;
    }

    private static String shortened(String message) {
        if (message == null || message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH);
    }

    public record RunCompletion(
            RunOutcome outcome, RunResult result, long runtimeMs, String message) {}
}
