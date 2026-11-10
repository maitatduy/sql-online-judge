package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.ProblemReview;
import com.sqljudge.problem.entity.ReviewDecision;
import java.time.Instant;

public record ProblemReviewResponse(ReviewDecision decision, String reason, Instant reviewedAt) {

    public static ProblemReviewResponse from(ProblemReview review) {
        return new ProblemReviewResponse(
                review.getDecision(), review.getReason(), review.getCreatedAt());
    }
}
