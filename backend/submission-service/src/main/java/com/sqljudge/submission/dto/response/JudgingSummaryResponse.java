package com.sqljudge.submission.dto.response;

import com.sqljudge.submission.repository.UserJudgingRow;
import java.util.List;

public record JudgingSummaryResponse(
        long submissionCount, long acceptedCount, List<UserJudgingRow> users) {}
