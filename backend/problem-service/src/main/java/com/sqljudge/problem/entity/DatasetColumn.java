package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "dataset_columns",
        indexes =
                @Index(
                        name = "uk_dataset_columns_table_id_name",
                        columnList = "table_id, name",
                        unique = true))
@Getter
@Setter
@NoArgsConstructor
public class DatasetColumn extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private DatasetTable table;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "logical_type", nullable = false, length = 16)
    private LogicalType logicalType;

    @Column(name = "is_primary_key", nullable = false)
    private boolean primaryKey;

    @Column(name = "is_nullable", nullable = false)
    private boolean nullable;

    @Column(name = "references_table", length = 64)
    private String referencesTable;

    @Column(name = "references_column", length = 64)
    private String referencesColumn;

    @Column(name = "position", nullable = false)
    private int position;

    DatasetColumn(DatasetTable table, String name, LogicalType logicalType, int position) {
        this.table = table;
        this.name = name;
        this.logicalType = logicalType;
        this.position = position;
    }
}
