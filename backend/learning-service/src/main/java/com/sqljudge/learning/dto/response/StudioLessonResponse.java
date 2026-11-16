package com.sqljudge.learning.dto.response;

import com.sqljudge.learning.entity.Lesson;
import java.util.UUID;

public record StudioLessonResponse(
        UUID id,
        UUID chapterId,
        String slug,
        String title,
        String body,
        int position,
        int estimatedMinutes) {

    public static StudioLessonResponse of(Lesson lesson) {
        return new StudioLessonResponse(
                lesson.getId(),
                lesson.getChapter().getId(),
                lesson.getSlug(),
                lesson.getTitle(),
                lesson.getBody(),
                lesson.getPosition(),
                lesson.getEstimatedMinutes());
    }
}
