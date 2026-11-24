package com.sqljudge.submission.dto.response;

import com.sqljudge.submission.entity.Dialect;
import com.sqljudge.submission.entity.Verdict;
import java.time.Instant;
import java.util.UUID;

public record PublicSubmissionResponse(
        UUID id,
        Verdict verdict,
        Integer problemNumber,
        String problemSlug,
        String problemTitle,
        Dialect dialect,
        Instant submittedAt) {}
