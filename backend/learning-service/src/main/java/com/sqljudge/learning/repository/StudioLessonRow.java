package com.sqljudge.learning.repository;

import java.util.UUID;

public record StudioLessonRow(
        UUID chapterId, UUID lessonId, String slug, String title, int estimatedMinutes) {}
