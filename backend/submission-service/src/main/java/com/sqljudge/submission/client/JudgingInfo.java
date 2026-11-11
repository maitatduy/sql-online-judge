package com.sqljudge.submission.client;

import com.sqljudge.submission.entity.Dialect;
import java.util.List;
import java.util.UUID;

public record JudgingInfo(
        UUID problemId,
        UUID revisionId,
        List<Dialect> dialects,
        int testcaseCount,
        int timeLimitMs) {

    public JudgingInfo {
        dialects = List.copyOf(dialects);
    }
}
