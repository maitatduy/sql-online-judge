package com.sqljudge.submission.dto.response;

import com.sqljudge.submission.client.UserName;
import com.sqljudge.submission.entity.Dialect;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.entity.Verdict;
import com.sqljudge.submission.repository.AdminSubmissionRow;
import java.time.Instant;
import java.util.UUID;

public record AdminSubmissionResponse(
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
        Instant createdAt) {

    public static AdminSubmissionResponse of(AdminSubmissionRow row, UserName user) {
        return new AdminSubmissionResponse(
                row.id(),
                row.userId(),
                user == null ? null : user.username(),
                user == null ? null : user.displayName(),
                row.problemId(),
                row.problemNumber(),
                row.problemSlug(),
                row.problemTitle(),
                row.contestId(),
                row.dialect(),
                row.status(),
                row.verdict(),
                row.runtimeMs(),
                row.createdAt());
    }
}
