package com.sqljudge.learning.repository;

import java.util.UUID;

public record PublishedLessonRow(
        UUID lessonId,
        UUID lessonKey,
        String slug,
        String title,
        String body,
        String chapterTitle,
        String courseSlug,
        String courseTitle) {}
