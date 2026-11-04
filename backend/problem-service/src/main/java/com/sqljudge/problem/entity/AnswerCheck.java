package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import com.sqljudge.problem.entity.AnswerCheckTestcase.TestcaseCheck;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "answer_checks",
        indexes =
                @Index(
                        name = "idx_answer_checks_problem_id_created_at",
                        columnList = "problem_id, created_at"))
@Getter
@NoArgsConstructor
public class AnswerCheck extends BaseEntity {

    @Column(name = "problem_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID problemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private ProblemRevision revision;

    @Column(name = "dataset_version_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID datasetVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AnswerCheckStatus status;

    @Column(name = "testcase_count", nullable = false)
    private int testcaseCount;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "check", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<AnswerCheckTestcase> testcases = new ArrayList<>();

    public static AnswerCheck pending(Problem problem, ProblemRevision revision) {
        AnswerCheck check = new AnswerCheck();
        check.problemId = problem.getId();
        check.revision = revision;
        check.datasetVersionId = revision.getDatasetVersion().getId();
        check.status = AnswerCheckStatus.PENDING;
        check.testcaseCount = revision.getTestcases().size();
        return check;
    }

    public boolean hasChecked(UUID testcaseId) {
        return testcases.stream().anyMatch(result -> result.getTestcaseId().equals(testcaseId));
    }

    public void record(Testcase testcase, TestcaseCheck result, Instant now) {
        testcases.add(new AnswerCheckTestcase(this, testcase, result));
        if (testcases.size() < testcaseCount) {
            return;
        }
        boolean passed = testcases.stream().allMatch(AnswerCheckTestcase::isPassed);
        status = passed ? AnswerCheckStatus.PASSED : AnswerCheckStatus.FAILED;
        completedAt = now;
    }
}
