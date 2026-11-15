package com.sqljudge.learning.dto.response;

import java.util.List;
import java.util.UUID;

public record CourseProgressResponse(
        boolean enrolled, List<UUID> completedLessonKeys, int lessonCount, String nextLessonSlug) {

    public CourseProgressResponse {
        completedLessonKeys = List.copyOf(completedLessonKeys);
    }
}
