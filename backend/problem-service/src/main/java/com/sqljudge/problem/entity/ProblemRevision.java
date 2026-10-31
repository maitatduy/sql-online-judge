package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "problem_revisions",
        indexes =
                @Index(
                        name = "uk_problem_revisions_problem_id_number",
                        columnList = "problem_id, number",
                        unique = true))
@Getter
@Setter
@NoArgsConstructor
public class ProblemRevision extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "checksum", columnDefinition = "CHAR(64)")
    private String checksum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_version_id")
    private DatasetVersion datasetVersion;

    @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReferenceSolution> referenceSolutions = new ArrayList<>();

    @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<Testcase> testcases = new ArrayList<>();

    ProblemRevision(Problem problem, int number) {
        this.problem = problem;
        this.number = number;
    }

    public void addReferenceSolution(ReferenceSolution solution) {
        solution.setRevision(this);
        referenceSolutions.add(solution);
    }

    public Testcase addTestcase(DatasetVariant variant) {
        Testcase testcase = new Testcase(this, testcases.size(), variant);
        testcases.add(testcase);
        return testcase;
    }
}
