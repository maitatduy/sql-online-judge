package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_variant_id", nullable = false)
    private DatasetVariant datasetVariant;

    @OneToOne(
            mappedBy = "testcase",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private ExpectedResult expectedResult;

    Testcase(ProblemRevision revision, int position, DatasetVariant datasetVariant) {
        this.revision = revision;
        this.position = position;
        this.datasetVariant = datasetVariant;
    }

    public boolean isPublic() {
        return datasetVariant.isPublic();
    }

    public void setExpectedResult(ExpectedResult result) {
        result.setTestcase(this);
        this.expectedResult = result;
    }
}
