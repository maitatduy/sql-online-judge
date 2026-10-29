package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "expected_results",
        indexes =
                @Index(
                        name = "uk_expected_results_testcase_id",
                        columnList = "testcase_id",
                        unique = true))
@Getter
@NoArgsConstructor
public class ExpectedResult extends BaseEntity {

    @Setter(AccessLevel.PACKAGE)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "testcase_id", nullable = false)
    private Testcase testcase;

    @Column(name = "columns_json", nullable = false, columnDefinition = "JSON")
    private String columnsJson;

    @Column(name = "rows_json", nullable = false, columnDefinition = "LONGTEXT")
    private String rowsJson;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "checksum", nullable = false, columnDefinition = "CHAR(64)")
    private String checksum;

    public ExpectedResult(String columnsJson, String rowsJson, int rowCount, String checksum) {
        this.columnsJson = columnsJson;
        this.rowsJson = rowsJson;
        this.rowCount = rowCount;
        this.checksum = checksum;
    }
}
