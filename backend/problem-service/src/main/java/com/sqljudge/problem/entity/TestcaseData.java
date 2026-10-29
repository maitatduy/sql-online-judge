package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "testcase_data",
        indexes =
                @Index(
                        name = "uk_testcase_data_testcase_id_table_name",
                        columnList = "testcase_id, table_name",
                        unique = true))
@Getter
@NoArgsConstructor
public class TestcaseData extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "testcase_id", nullable = false)
    private Testcase testcase;

    @Column(name = "table_name", nullable = false, length = 64)
    private String tableName;

    @Column(name = "csv", nullable = false, columnDefinition = "LONGTEXT")
    private String csv;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    TestcaseData(Testcase testcase, String tableName, String csv, int rowCount) {
        this.testcase = testcase;
        this.tableName = tableName;
        this.csv = csv;
        this.rowCount = rowCount;
    }
}
