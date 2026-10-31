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
        name = "dataset_versions",
        indexes =
                @Index(
                        name = "uk_dataset_versions_dataset_id_number",
                        columnList = "dataset_id, number",
                        unique = true))
@Getter
@Setter
@NoArgsConstructor
public class DatasetVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "checksum", columnDefinition = "CHAR(64)")
    private String checksum;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<DatasetTable> tables = new ArrayList<>();

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<DatasetVariant> variants = new ArrayList<>();

    DatasetVersion(Dataset dataset, int number) {
        this.dataset = dataset;
        this.number = number;
    }

    public boolean isDraft() {
        return frozenAt == null;
    }

    public DatasetTable addTable(String name) {
        DatasetTable table = new DatasetTable(this, name, tables.size());
        tables.add(table);
        return table;
    }

    public DatasetVariant addVariant(String name, boolean isPublic) {
        DatasetVariant variant = new DatasetVariant(this, name, variants.size(), isPublic);
        variants.add(variant);
        return variant;
    }
}
