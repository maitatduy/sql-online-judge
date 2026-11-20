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
        name = "quiz_options",
        indexes = {
            @Index(
                    name = "uk_quiz_options_question_id_position",
                    columnList = "question_id, position",
                    unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
public class QuizOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "option_key", nullable = false, columnDefinition = "BINARY(16)")
    private UUID optionKey;

    @Column(name = "label", nullable = false, length = 300)
    private String label;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "position", nullable = false)
    private int position;

    public static QuizOption of(QuizQuestion question, int position) {
        QuizOption option = new QuizOption();
        option.question = question;
        option.optionKey = UUID.randomUUID();
        option.position = position;
        return option;
    }

    public void describe(String label, boolean correct) {
        this.label = label;
        this.correct = correct;
    }
}
