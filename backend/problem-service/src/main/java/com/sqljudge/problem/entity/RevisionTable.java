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
        name = "revision_tables",
        indexes =
                @Index(
                        name = "uk_revision_tables_revision_id_name",
                        columnList = "revision_id, name",
                        unique = true))
@Getter
@NoArgsConstructor
public class RevisionTable extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private ProblemRevision revision;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "position", nullable = false)
    private int position;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<RevisionColumn> columns = new ArrayList<>();

    RevisionTable(ProblemRevision revision, String name, int position) {
        this.revision = revision;
        this.name = name;
        this.position = position;
    }

    public RevisionColumn addColumn(String columnName, LogicalType logicalType) {
        RevisionColumn column = new RevisionColumn(this, columnName, logicalType, columns.size());
        columns.add(column);
        return column;
    }
}
