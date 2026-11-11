package com.sqljudge.submission.dto.response;

import com.sqljudge.submission.entity.Dialect;
import com.sqljudge.submission.entity.Submission;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.entity.Verdict;
import java.time.Instant;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        UUID problemId,
        Dialect dialect,
        SubmissionStatus status,
        Verdict verdict,
        int judgedTestcases,
        int totalTestcases,
        Integer runtimeMs,
        String errorMessage,
        FailedTestcase firstFailedTestcase,
        Instant submittedAt) {

    public record FailedTestcase(int index, boolean isHidden) {}

    public static SubmissionResponse from(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getProblemId(),
                submission.getDialect(),
                submission.getStatus(),
                submission.getVerdict(),
                submission.getJudgedTestcases(),
                submission.getTotalTestcases(),
                submission.getRuntimeMs(),
                submission.getErrorMessage(),
                failedTestcaseOf(submission),
                submission.getCreatedAt());
    }

    private static FailedTestcase failedTestcaseOf(Submission submission) {
        if (submission.getFirstFailedIndex() == null) {
            return null;
        }
        return new FailedTestcase(
                submission.getFirstFailedIndex(),
                Boolean.TRUE.equals(submission.getFirstFailedIsHidden()));
    }
}
