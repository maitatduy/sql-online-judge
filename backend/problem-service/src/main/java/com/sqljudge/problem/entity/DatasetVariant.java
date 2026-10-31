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
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "dataset_variants",
        indexes =
                @Index(
                        name = "uk_dataset_variants_version_id_name",
                        columnList = "version_id, name",
                        unique = true))
@Getter
@Setter
@NoArgsConstructor
public class DatasetVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private DatasetVersion version;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DatasetVariantTable> tables = new ArrayList<>();

    DatasetVariant(DatasetVersion version, String name, int position, boolean isPublic) {
        this.version = version;
        this.name = name;
        this.position = position;
        this.isPublic = isPublic;
    }

    public void addTable(String tableName, String csv, int rowCount) {
        tables.add(new DatasetVariantTable(this, tableName, csv, rowCount));
    }

    public Optional<DatasetVariantTable> findTable(String tableName) {
        return tables.stream().filter(table -> table.getTableName().equals(tableName)).findFirst();
    }

    public void putTable(String tableName, String csv, int rowCount) {
        findTable(tableName)
                .ifPresentOrElse(
                        table -> table.replaceData(csv, rowCount),
                        () -> addTable(tableName, csv, rowCount));
    }

    public void keepTablesNamed(Set<String> tableNames) {
        tables.removeIf(table -> !tableNames.contains(table.getTableName()));
    }
}
