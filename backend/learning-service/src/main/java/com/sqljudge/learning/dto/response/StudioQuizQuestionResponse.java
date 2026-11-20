package com.sqljudge.learning.dto.response;

import com.sqljudge.learning.entity.QuizOption;
import com.sqljudge.learning.entity.QuizQuestion;
import java.util.List;
import java.util.UUID;

public record StudioQuizQuestionResponse(
        UUID id, int position, String prompt, boolean multiple, List<Option> options) {

    public StudioQuizQuestionResponse {
        options = List.copyOf(options);
    }

    public static StudioQuizQuestionResponse of(QuizQuestion question, List<QuizOption> options) {
        return new StudioQuizQuestionResponse(
                question.getId(),
                question.getPosition(),
                question.getPrompt(),
                question.isMultiple(),
                options.stream()
                        .map(
                                option ->
                                        new Option(
                                                option.getId(),
                                                option.getLabel(),
                                                option.isCorrect(),
                                                option.getPosition()))
                        .toList());
    }

    public record Option(UUID id, String label, boolean correct, int position) {}
}
