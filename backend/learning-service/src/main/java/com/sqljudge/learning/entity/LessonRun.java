package com.sqljudge.learning.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "lesson_runs",
        indexes = {
            @Index(name = "idx_lesson_runs_user_id_created_at", columnList = "user_id, created_at")
        })
@Getter
@NoArgsConstructor
public class LessonRun extends BaseEntity {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "cell_key", nullable = false, columnDefinition = "BINARY(16)")
    private UUID cellKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "dialect", nullable = false, length = 20)
    private Dialect dialect;

    @Column(name = "sql_text", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String sql;

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

    public static LessonRun pending(UUID userId, UUID cellKey, Dialect dialect, String sql) {
        LessonRun run = new LessonRun();
        run.userId = userId;
        run.cellKey = cellKey;
        run.dialect = dialect;
        run.sql = sql;
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
