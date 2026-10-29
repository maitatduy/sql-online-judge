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
        name = "reference_solutions",
        indexes =
                @Index(
                        name = "uk_reference_solutions_revision_id_dialect",
                        columnList = "revision_id, dialect",
                        unique = true))
@Getter
@Setter
@NoArgsConstructor
public class ReferenceSolution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private ProblemRevision revision;

    @Enumerated(EnumType.STRING)
    @Column(name = "dialect", nullable = false, length = 16)
    private Dialect dialect;

    @Column(name = "code", nullable = false, columnDefinition = "TEXT")
    private String code;

    public ReferenceSolution(Dialect dialect, String code) {
        this.dialect = dialect;
        this.code = code;
    }
}
