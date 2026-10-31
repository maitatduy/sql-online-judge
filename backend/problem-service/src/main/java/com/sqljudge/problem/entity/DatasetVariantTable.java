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
        name = "dataset_variant_tables",
        indexes =
                @Index(
                        name = "uk_dataset_variant_tables_variant_id_table_name",
                        columnList = "variant_id, table_name",
                        unique = true))
@Getter
@NoArgsConstructor
public class DatasetVariantTable extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private DatasetVariant variant;

    @Column(name = "table_name", nullable = false, length = 64)
    private String tableName;

    @Column(name = "csv", nullable = false, columnDefinition = "LONGTEXT")
    private String csv;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    DatasetVariantTable(DatasetVariant variant, String tableName, String csv, int rowCount) {
        this.variant = variant;
        this.tableName = tableName;
        this.csv = csv;
        this.rowCount = rowCount;
    }
}
