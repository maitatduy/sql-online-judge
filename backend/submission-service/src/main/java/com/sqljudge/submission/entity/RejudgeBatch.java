package com.sqljudge.submission.entity;

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

@Entity
@Table(
        name = "rejudge_batches",
        indexes =
                @Index(
                        name = "idx_rejudge_batches_problem_id_status",
                        columnList = "problem_id, status"))
@Getter
@NoArgsConstructor
public class RejudgeBatch extends BaseEntity {

    @Column(name = "problem_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID problemId;

    @Column(name = "problem_revision_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID problemRevisionId;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "remaining_count", nullable = false)
    private int remainingCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RejudgeStatus status;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public static RejudgeBatch start(UUID problemId, UUID revisionId, int totalCount) {
        RejudgeBatch batch = new RejudgeBatch();
        batch.problemId = problemId;
        batch.problemRevisionId = revisionId;
        batch.totalCount = totalCount;
        batch.remainingCount = totalCount;
        batch.status = RejudgeStatus.RUNNING;
        return batch;
    }

    public boolean finishIfDone(Instant now) {
        if (status != RejudgeStatus.RUNNING || remainingCount > 0) {
            return false;
        }
        status = RejudgeStatus.FINISHED;
        finishedAt = now;
        return true;
    }
}
