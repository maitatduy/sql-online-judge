package com.sqljudge.contest.entity;

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

@Entity
@Table(
        name = "contest_problems",
        indexes = {
            @Index(
                    name = "uk_contest_problems_contest_id_problem_id",
                    columnList = "contest_id, problem_id",
                    unique = true),
            @Index(
                    name = "uk_contest_problems_contest_id_position",
                    columnList = "contest_id, position",
                    unique = true)
        })
@Getter
@NoArgsConstructor
public class ContestProblem extends BaseEntity {

    private static final char FIRST_LETTER = 'A';

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @Column(name = "problem_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID problemId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "difficulty", nullable = false, length = 10)
    private String difficulty;

    ContestProblem(Contest contest, int position, ContestProblemInfo info) {
        this.contest = contest;
        this.position = position;
        this.problemId = info.problemId();
        this.slug = info.slug();
        this.title = info.title();
        this.difficulty = info.difficulty();
    }

    public String letter() {
        return String.valueOf((char) (FIRST_LETTER + position));
    }
}
