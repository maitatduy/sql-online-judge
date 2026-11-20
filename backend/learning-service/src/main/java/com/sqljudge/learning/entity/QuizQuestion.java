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
        name = "quiz_questions",
        indexes = {
            @Index(name = "idx_quiz_questions_question_key", columnList = "question_key"),
            @Index(
                    name = "uk_quiz_questions_lesson_id_position",
                    columnList = "lesson_id, position",
                    unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
public class QuizQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "question_key", nullable = false, columnDefinition = "BINARY(16)")
    private UUID questionKey;

    @Column(name = "prompt", nullable = false, length = 500)
    private String prompt;

    @Column(name = "is_multiple", nullable = false)
    private boolean multiple;

    @Column(name = "position", nullable = false)
    private int position;

    public static QuizQuestion of(Lesson lesson, int position) {
        QuizQuestion question = new QuizQuestion();
        question.lesson = lesson;
        question.questionKey = UUID.randomUUID();
        question.position = position;
        return question;
    }

    public void ask(String prompt, boolean multiple) {
        this.prompt = prompt;
        this.multiple = multiple;
    }
}
