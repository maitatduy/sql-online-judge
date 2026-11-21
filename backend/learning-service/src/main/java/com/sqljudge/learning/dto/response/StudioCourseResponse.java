package com.sqljudge.learning.dto.response;

import com.sqljudge.learning.entity.Course;
import com.sqljudge.learning.entity.CourseReview;
import com.sqljudge.learning.entity.CourseReviewDecision;
import com.sqljudge.learning.entity.CourseRevision;
import com.sqljudge.learning.entity.CourseRevisionStatus;
import com.sqljudge.learning.entity.CourseStatus;
import com.sqljudge.learning.entity.Difficulty;
import java.time.Instant;
import java.util.UUID;

public record StudioCourseResponse(
        UUID id,
        String slug,
        CourseStatus status,
        UUID revisionId,
        CourseRevisionStatus revisionStatus,
        String title,
        String summary,
        Difficulty difficulty,
        int estimatedMinutes,
        int lessonCount,
        Instant submittedAt,
        Instant updatedAt,
        LatestReview latestReview) {

    public static StudioCourseResponse of(Course course, CourseRevision revision) {
        return new StudioCourseResponse(
                course.getId(),
                course.getSlug(),
                course.getStatus(),
                revision.getId(),
                revision.getStatus(),
                revision.getTitle(),
                revision.getSummary(),
                revision.getDifficulty(),
                revision.getEstimatedMinutes(),
                revision.getLessonCount(),
                revision.getSubmittedAt(),
                revision.getUpdatedAt(),
                null);
    }

    public StudioCourseResponse withReview(CourseReview review) {
        LatestReview latest =
                review == null
                        ? null
                        : new LatestReview(
                                review.getDecision(), review.getReason(), review.getCreatedAt());
        return new StudioCourseResponse(
                id,
                slug,
                status,
                revisionId,
                revisionStatus,
                title,
                summary,
                difficulty,
                estimatedMinutes,
                lessonCount,
                submittedAt,
                updatedAt,
                latest);
    }

    public record LatestReview(CourseReviewDecision decision, String reason, Instant reviewedAt) {}
}
