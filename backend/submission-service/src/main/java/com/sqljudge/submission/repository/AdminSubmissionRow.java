package com.sqljudge.submission.repository;

import com.sqljudge.submission.entity.Dialect;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.entity.Verdict;
import java.time.Instant;
import java.util.UUID;

public record AdminSubmissionRow(
        UUID id,
        UUID userId,
        UUID problemId,
        Integer problemNumber,
        String problemSlug,
        String problemTitle,
        UUID contestId,
        Dialect dialect,
        SubmissionStatus status,
        Verdict verdict,
        Integer runtimeMs,
        Instant createdAt) {}
