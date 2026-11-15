package com.sqljudge.learning.dto.response;

import com.sqljudge.learning.entity.Difficulty;

public record EnrolledCourseResponse(
        String slug,
        String title,
        Difficulty difficulty,
        int completedCount,
        int lessonCount,
        String nextLessonSlug) {}
