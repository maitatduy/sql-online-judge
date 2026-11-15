package com.sqljudge.learning.dto.response;

import java.util.List;
import java.util.UUID;

public record StudioChapterResponse(
        UUID id, String title, int position, List<LessonOutline> lessons) {

    public StudioChapterResponse {
        lessons = List.copyOf(lessons);
    }

    public record LessonOutline(UUID id, String slug, String title, int estimatedMinutes) {}
}
