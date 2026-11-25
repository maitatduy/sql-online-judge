package com.sqljudge.submission.repository;

import com.sqljudge.submission.entity.Dialect;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.entity.Verdict;
import java.util.UUID;

public record AdminSubmissionFilter(
        Verdict verdict,
        SubmissionStatus status,
        Dialect dialect,
        SubmissionSource source,
        UUID userId,
        String problemSlug) {}
