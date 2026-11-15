package com.sqljudge.learning.dto.response;

import com.sqljudge.learning.entity.LessonRun;
import com.sqljudge.learning.entity.RunOutcome;
import com.sqljudge.learning.entity.RunResult;
import com.sqljudge.learning.entity.RunStatus;
import java.util.UUID;

public record LessonRunResponse(
        UUID id,
        RunStatus status,
        RunOutcome outcome,
        RunResult result,
        String errorMessage,
        Integer runtimeMs) {

    public static LessonRunResponse from(LessonRun run) {
        return new LessonRunResponse(
                run.getId(),
                run.getStatus(),
                run.getOutcome(),
                run.getResult(),
                run.getErrorMessage(),
                run.getRuntimeMs());
    }
}
