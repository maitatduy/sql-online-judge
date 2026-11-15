package com.sqljudge.learning.dto.response;

import java.util.List;
import java.util.UUID;

public record QuizResultResponse(
        boolean allCorrect, List<QuestionResult> results, CourseProgressResponse progress) {

    public QuizResultResponse {
        results = List.copyOf(results);
    }

    public record QuestionResult(UUID questionKey, boolean correct) {}
}
