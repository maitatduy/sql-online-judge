package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "studio_runs",
        indexes =
                @Index(
                        name = "idx_studio_runs_problem_id_created_at",
                        columnList = "problem_id, created_at"))
@Getter
@NoArgsConstructor
public class StudioRun extends BaseEntity {

    @Column(name = "problem_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID problemId;

    @Column(name = "dataset_version_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID datasetVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dialect", nullable = false, length = 16)
    private Dialect dialect;

    @Column(name = "sql_code", nullable = false, columnDefinition = "TEXT")
    private String sqlCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private StudioRunStatus status;

    @Column(name = "result_json", columnDefinition = "LONGTEXT")
    private String resultJson;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static StudioRun pending(
            UUID problemId, UUID datasetVersionId, Dialect dialect, String sqlCode) {
        StudioRun run = new StudioRun();
        run.problemId = problemId;
        run.datasetVersionId = datasetVersionId;
        run.dialect = dialect;
        run.sqlCode = sqlCode;
        run.status = StudioRunStatus.PENDING;
        return run;
    }

    public boolean isCompleted() {
        return status == StudioRunStatus.COMPLETED;
    }

    public void complete(String resultJson, Instant completedAt) {
        this.resultJson = resultJson;
        this.completedAt = completedAt;
        this.status = StudioRunStatus.COMPLETED;
    }
}
