package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "problems",
        indexes = {
            @Index(name = "uk_problems_slug", columnList = "slug", unique = true),
            @Index(name = "uk_problems_number", columnList = "number", unique = true),
            @Index(
                    name = "idx_problems_status_visibility_number",
                    columnList = "status, visibility, number"),
            @Index(name = "idx_problems_author_id_updated_at", columnList = "author_id, updated_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class Problem extends BaseEntity {

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "number")
    private Integer number;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 10)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProblemStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", length = 20)
    private ProblemVisibility visibility;

    @Column(name = "author_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID authorId;

    @Column(name = "statement_markdown", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String statementMarkdown;

    @Column(name = "starter_code", nullable = false, columnDefinition = "TEXT")
    private String starterCode;

    @Column(name = "time_limit_ms", nullable = false)
    private int timeLimitMs;

    @Column(name = "requires_order", nullable = false)
    private boolean requiresOrder;

    @Column(name = "compare_column_names", nullable = false)
    private boolean compareColumnNames;

    @Column(name = "float_tolerance", nullable = false)
    private double floatTolerance = 1e-6;

    @Convert(converter = StatementTypesConverter.class)
    @Column(name = "allowed_statements", nullable = false, length = 64)
    private Set<StatementType> allowedStatements =
            EnumSet.of(StatementType.SELECT, StatementType.WITH);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_revision_id")
    private ProblemRevision draftRevision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_revision_id")
    private ProblemRevision publishedRevision;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "problem_tags",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProblemRevision> revisions = new ArrayList<>();

    public ProblemRevision addRevision(int revisionNumber) {
        ProblemRevision revision = new ProblemRevision(this, revisionNumber);
        revisions.add(revision);
        return revision;
    }
}
