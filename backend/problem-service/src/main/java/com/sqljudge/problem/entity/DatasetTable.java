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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "dataset_tables",
        indexes =
                @Index(
                        name = "uk_dataset_tables_version_id_name",
                        columnList = "version_id, name",
                        unique = true))
@Getter
@NoArgsConstructor
public class DatasetTable extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private DatasetVersion version;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "position", nullable = false)
    private int position;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<DatasetColumn> columns = new ArrayList<>();

    DatasetTable(DatasetVersion version, String name, int position) {
        this.version = version;
        this.name = name;
        this.position = position;
    }

    public DatasetColumn addColumn(String columnName, LogicalType logicalType) {
        DatasetColumn column = new DatasetColumn(this, columnName, logicalType, columns.size());
        columns.add(column);
        return column;
    }
}
