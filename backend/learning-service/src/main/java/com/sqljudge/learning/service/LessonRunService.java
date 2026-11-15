package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.learning.dto.request.StartLessonRunRequest;
import com.sqljudge.learning.dto.response.LessonRunAcceptedResponse;
import com.sqljudge.learning.dto.response.LessonRunResponse;
import com.sqljudge.learning.entity.LessonCell;
import com.sqljudge.learning.entity.LessonRun;
import com.sqljudge.learning.entity.LessonRun.RunCompletion;
import com.sqljudge.learning.repository.LessonCellRepository;
import com.sqljudge.learning.repository.LessonRunRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonRunService {

    private final LessonCellRepository lessonCellRepository;
    private final LessonRunRepository lessonRunRepository;
    private final LessonRunWriter lessonRunWriter;
    private final Clock clock;

    public LessonRunAcceptedResponse start(UUID cellKey, StartLessonRunRequest request) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        LessonCell cell =
                lessonCellRepository
                        .findPublishedByCellKey(cellKey)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.LESSON_CELL_NOT_FOUND,
                                                "Không tìm thấy ô SQL"));
        LessonRun run =
                lessonRunWriter.create(
                        LessonRun.pending(userId, cellKey, cell.getDialect(), request.sql()), cell);
        log.info(
                "lesson_run_requested runId={} cellKey={} sqlLength={}",
                run.getId(),
                cellKey,
                request.sql().length());
        return new LessonRunAcceptedResponse(run.getId());
    }

    @Transactional(readOnly = true)
    public LessonRunResponse get(UUID runId) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        return lessonRunRepository
                .findByIdAndUserId(runId, userId)
                .map(LessonRunResponse::from)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.LESSON_RUN_NOT_FOUND, "Không tìm thấy lần chạy"));
    }

    @Transactional
    public void complete(UUID runId, RunCompletion completion) {
        lessonRunRepository
                .findById(runId)
                .ifPresentOrElse(
                        run -> run.complete(completion, clock.instant()),
                        () -> log.warn("lesson_run_result_unknown_run runId={}", runId));
    }
}
