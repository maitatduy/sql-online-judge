package com.sqljudge.submission.dto.response;

public record SubmissionStatsResponse(
        long submissionCount, long acceptedCount, long solvedCount, long solvedThisWeek) {}
