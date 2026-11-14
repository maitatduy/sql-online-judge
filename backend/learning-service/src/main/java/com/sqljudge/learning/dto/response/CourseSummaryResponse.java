package com.sqljudge.learning.dto.response;

import com.sqljudge.learning.entity.Difficulty;
import java.util.UUID;

public record CourseSummaryResponse(
        UUID id,
        String slug,
        String title,
        String summary,
        Difficulty difficulty,
        int lessonCount,
        int estimatedMinutes) {}
