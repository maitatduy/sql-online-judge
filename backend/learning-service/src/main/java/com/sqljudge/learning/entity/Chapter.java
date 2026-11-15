package com.sqljudge.learning.entity;

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
import lombok.Setter;

@Entity
@Table(
        name = "chapters",
        indexes = {
            @Index(
                    name = "uk_chapters_revision_id_position",
                    columnList = "revision_id, position",
                    unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
public class Chapter extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private CourseRevision revision;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "position", nullable = false)
    private int position;

    public static Chapter of(CourseRevision revision, String title, int position) {
        Chapter chapter = new Chapter();
        chapter.revision = revision;
        chapter.title = title;
        chapter.position = position;
        return chapter;
    }
}
