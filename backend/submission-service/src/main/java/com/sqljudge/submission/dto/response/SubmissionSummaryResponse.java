package com.sqljudge.submission.dto.response;

import com.sqljudge.submission.entity.Dialect;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.entity.Verdict;
import java.time.Instant;
import java.util.UUID;

public record SubmissionSummaryResponse(
        UUID id,
        SubmissionStatus status,
        Verdict verdict,
        Dialect dialect,
        Integer runtimeMs,
        Instant submittedAt) {}
