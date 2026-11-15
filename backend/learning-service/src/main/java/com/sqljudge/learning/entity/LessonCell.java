package com.sqljudge.learning.entity;

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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "lesson_cells",
        indexes = {
            @Index(name = "idx_lesson_cells_cell_key", columnList = "cell_key"),
            @Index(
                    name = "uk_lesson_cells_lesson_id_position",
                    columnList = "lesson_id, position",
                    unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
public class LessonCell extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "cell_key", nullable = false, columnDefinition = "BINARY(16)")
    private UUID cellKey;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "starter_sql", nullable = false, columnDefinition = "TEXT")
    private String starterSql;

    @Enumerated(EnumType.STRING)
    @Column(name = "dialect", nullable = false, length = 20)
    private Dialect dialect;

    @Column(name = "dataset_version_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID datasetVersionId;

    @Column(name = "dataset_variant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID datasetVariantId;

    @Column(name = "dataset_checksum", nullable = false, length = 64)
    private String datasetChecksum;
}
