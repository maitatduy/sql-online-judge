package com.sqljudge.submission.dto.response;

import com.sqljudge.submission.entity.RejudgeBatch;
import com.sqljudge.submission.entity.RejudgeStatus;
import java.util.UUID;

public record RejudgeBatchResponse(
        UUID batchId, RejudgeStatus status, int totalCount, int remainingCount) {

    public static RejudgeBatchResponse from(RejudgeBatch batch) {
        return new RejudgeBatchResponse(
                batch.getId(), batch.getStatus(), batch.getTotalCount(), batch.getRemainingCount());
    }
}
