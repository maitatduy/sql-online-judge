package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "problem_stats",
        indexes =
                @Index(
                        name = "uk_problem_stats_problem_id",
                        columnList = "problem_id",
                        unique = true))
@Getter
@NoArgsConstructor
public class ProblemStats extends BaseEntity {

    private static final double PERCENT_TENTHS = 1000.0;
    private static final double TENTHS = 10.0;

    @Column(name = "problem_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID problemId;

    @Column(name = "submission_count", nullable = false)
    private long submissionCount;

    @Column(name = "accepted_count", nullable = false)
    private long acceptedCount;

    @Column(name = "solved_user_count", nullable = false)
    private long solvedUserCount;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public ProblemStats(
            UUID problemId, long submissionCount, long acceptedCount, long solvedUserCount) {
        this.problemId = problemId;
        this.submissionCount = submissionCount;
        this.acceptedCount = acceptedCount;
        this.solvedUserCount = solvedUserCount;
    }

    public double acceptanceRate() {
        return acceptanceRate(submissionCount, acceptedCount);
    }

    public static double acceptanceRate(long submissionCount, long acceptedCount) {
        if (submissionCount == 0) {
            return 0;
        }
        return Math.round(acceptedCount * PERCENT_TENTHS / submissionCount) / TENTHS;
    }
}
