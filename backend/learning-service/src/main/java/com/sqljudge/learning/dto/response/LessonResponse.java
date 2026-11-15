package com.sqljudge.learning.dto.response;

import java.util.List;
import java.util.UUID;

public record LessonResponse(
        UUID lessonKey,
        String slug,
        String title,
        String body,
        String chapterTitle,
        String courseSlug,
        String courseTitle,
        List<LessonCellResponse> cells) {

    public LessonResponse {
        cells = List.copyOf(cells);
    }
}
