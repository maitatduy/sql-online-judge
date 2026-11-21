package com.sqljudge.learning.dto.response;

import com.sqljudge.learning.entity.Difficulty;
import java.time.Instant;
import java.util.UUID;

public record CourseReviewQueueItem(
        UUID courseId,
        String slug,
        String title,
        Difficulty difficulty,
        int lessonCount,
        int estimatedMinutes,
        UUID authorId,
        Instant submittedAt) {}
