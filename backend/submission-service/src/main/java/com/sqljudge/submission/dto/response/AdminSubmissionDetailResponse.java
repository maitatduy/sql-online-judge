package com.sqljudge.submission.dto.response;

import com.sqljudge.submission.client.UserName;
import com.sqljudge.submission.entity.Dialect;
import com.sqljudge.submission.entity.Submission;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.entity.Verdict;
import java.time.Instant;
import java.util.UUID;

public record AdminSubmissionDetailResponse(
        UUID id,
        UUID userId,
        String username,
        String displayName,
        UUID problemId,
        Integer problemNumber,
        String problemSlug,
        String problemTitle,
        UUID contestId,
        Dialect dialect,
        SubmissionStatus status,
        Verdict verdict,
        Integer runtimeMs,
        String code,
        String errorMessage,
        Integer firstFailedIndex,
        int judgedTestcases,
        int totalTestcases,
        int attempts,
        Instant createdAt,
        Instant judgedAt) {

    public static AdminSubmissionDetailResponse of(Submission submission, UserName user) {
        return new AdminSubmissionDetailResponse(
                submission.getId(),
                submission.getUserId(),
                user == null ? null : user.username(),
                user == null ? null : user.displayName(),
                submission.getProblemId(),
                submission.getProblemNumber(),
                submission.getProblemSlug(),
                submission.getProblemTitle(),
                submission.getContestId(),
                submission.getDialect(),
                submission.getStatus(),
                submission.getVerdict(),
                submission.getRuntimeMs(),
                submission.getCode(),
                submission.getErrorMessage(),
                submission.getFirstFailedIndex(),
                submission.getJudgedTestcases(),
                submission.getTotalTestcases(),
                submission.getAttempts(),
                submission.getCreatedAt(),
                submission.getJudgedAt());
    }
}
