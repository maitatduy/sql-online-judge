package com.sqljudge.learning.dto.response;

import com.sqljudge.learning.entity.Difficulty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CourseDetailResponse(
        UUID id,
        String slug,
        String title,
        String summary,
        Difficulty difficulty,
        int lessonCount,
        int estimatedMinutes,
        Instant publishedAt,
        List<ChapterOutline> chapters) {

    public CourseDetailResponse {
        chapters = List.copyOf(chapters);
    }

    public record ChapterOutline(UUID id, String title, List<LessonOutline> lessons) {

        public ChapterOutline {
            lessons = List.copyOf(lessons);
        }
    }

    public record LessonOutline(UUID lessonKey, String slug, String title) {}
}
