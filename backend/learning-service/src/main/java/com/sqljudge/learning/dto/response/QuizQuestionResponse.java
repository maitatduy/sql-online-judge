package com.sqljudge.learning.dto.response;

import java.util.List;
import java.util.UUID;

public record QuizQuestionResponse(
        UUID questionKey, String prompt, boolean multiple, List<Option> options) {

    public QuizQuestionResponse {
        options = List.copyOf(options);
    }

    public record Option(UUID optionKey, String label) {}
}
