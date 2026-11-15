package com.sqljudge.learning.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "lessons",
        indexes = {
            @Index(name = "idx_lessons_lesson_key", columnList = "lesson_key"),
            @Index(
                    name = "uk_lessons_chapter_id_position",
                    columnList = "chapter_id, position",
                    unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(name = "lesson_key", nullable = false, columnDefinition = "BINARY(16)")
    private UUID lessonKey;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String body;

    @Column(name = "position", nullable = false)
    private int position;
}
