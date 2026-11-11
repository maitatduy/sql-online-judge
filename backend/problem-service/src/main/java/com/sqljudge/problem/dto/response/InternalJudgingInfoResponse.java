package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.entity.Dialect;
import java.util.List;
import java.util.UUID;

public record InternalJudgingInfoResponse(
        UUID problemId,
        UUID revisionId,
        List<Dialect> dialects,
        int testcaseCount,
        int timeLimitMs) {

    public InternalJudgingInfoResponse {
        dialects = List.copyOf(dialects);
    }
}
