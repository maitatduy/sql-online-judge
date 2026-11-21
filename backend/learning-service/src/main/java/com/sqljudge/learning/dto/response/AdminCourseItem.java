package com.sqljudge.learning.dto.response;

import com.sqljudge.learning.entity.CourseRevisionStatus;
import com.sqljudge.learning.entity.CourseStatus;
import com.sqljudge.learning.entity.Difficulty;
import java.time.Instant;
import java.util.UUID;

public record AdminCourseItem(
        UUID courseId,
        String slug,
        String title,
        Difficulty difficulty,
        CourseStatus status,
        CourseRevisionStatus revisionStatus,
        int lessonCount,
        UUID authorId,
        Instant updatedAt,
        Instant publishedAt) {}
