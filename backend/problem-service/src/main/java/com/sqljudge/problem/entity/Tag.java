package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "tags",
        indexes = {
            @Index(name = "uk_tags_slug", columnList = "slug", unique = true),
            @Index(name = "uk_tags_name", columnList = "name", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
public class Tag extends BaseEntity {

    @Column(name = "slug", nullable = false, length = 60)
    private String slug;

    @Column(name = "name", nullable = false, length = 60)
    private String name;
}
