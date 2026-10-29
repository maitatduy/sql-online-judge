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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "testcases",
        indexes =
                @Index(
                        name = "idx_testcases_revision_id_position",
                        columnList = "revision_id, position"))
@Getter
@NoArgsConstructor
public class Testcase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private ProblemRevision revision;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @OneToMany(mappedBy = "testcase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestcaseData> data = new ArrayList<>();

    @OneToOne(
            mappedBy = "testcase",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private ExpectedResult expectedResult;

    Testcase(ProblemRevision revision, int position, String description, boolean isPublic) {
        this.revision = revision;
        this.position = position;
        this.description = description;
        this.isPublic = isPublic;
    }

    public void addData(String tableName, String csv, int rowCount) {
        data.add(new TestcaseData(this, tableName, csv, rowCount));
    }

    public void setExpectedResult(ExpectedResult result) {
        result.setTestcase(this);
        this.expectedResult = result;
    }
}
