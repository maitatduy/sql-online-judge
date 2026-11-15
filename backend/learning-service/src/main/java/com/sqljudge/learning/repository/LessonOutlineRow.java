package com.sqljudge.learning.repository;

import java.util.UUID;

public record LessonOutlineRow(
        UUID chapterId, String chapterTitle, UUID lessonKey, String slug, String title) {}
