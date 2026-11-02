package com.sqljudge.problem.dto.response;

import com.sqljudge.problem.dto.response.StudioRunOutcome.Rejection;
import com.sqljudge.problem.dto.response.StudioRunOutcome.VariantResult;
import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.StudioRun;
import com.sqljudge.problem.entity.StudioRunStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudioRunResponse(
        UUID id,
        Dialect dialect,
        StudioRunStatus status,
        String sql,
        Instant createdAt,
        Instant completedAt,
        Rejection rejection,
        String systemError,
        List<VariantResult> variants) {

    public static StudioRunResponse of(StudioRun run, StudioRunOutcome outcome) {
        return new StudioRunResponse(
                run.getId(),
                run.getDialect(),
                run.getStatus(),
                run.getSqlCode(),
                run.getCreatedAt(),
                run.getCompletedAt(),
                outcome.rejection(),
                outcome.systemError(),
                outcome.variants());
    }
}
