package com.sqljudge.submission.dto.response;

import com.sqljudge.submission.entity.CodeRun;
import com.sqljudge.submission.entity.RunOutcome;
import com.sqljudge.submission.entity.RunResult;
import com.sqljudge.submission.entity.RunStatus;
import java.util.UUID;

public record CodeRunResponse(
        UUID id,
        RunStatus status,
        RunOutcome outcome,
        RunResult result,
        String errorMessage,
        Integer runtimeMs) {

    public static CodeRunResponse from(CodeRun run) {
        return new CodeRunResponse(
                run.getId(),
                run.getStatus(),
                run.getOutcome(),
                run.getResult(),
                run.getErrorMessage(),
                run.getRuntimeMs());
    }
}
