package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.AnswerCheck;
import com.sqljudge.problem.entity.AnswerCheckStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnswerCheckResponse(
        UUID id,
        AnswerCheckStatus status,
        int testcaseCount,
        int checkedCount,
        Instant createdAt,
        Instant completedAt,
        List<TestcaseResult> testcases) {

    public record TestcaseResult(
            UUID testcaseId,
            int position,
            boolean isPublic,
            boolean isPassed,
            String systemError,
            List<DialectCheckResult> dialects) {}

    public static AnswerCheckResponse of(AnswerCheck check, List<TestcaseResult> testcases) {
        return new AnswerCheckResponse(
                check.getId(),
                check.getStatus(),
                check.getTestcaseCount(),
                testcases.size(),
                check.getCreatedAt(),
                check.getCompletedAt(),
                testcases);
    }
}
