package com.sqljudge.problem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.outbox.OutboxWriter;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.problem.dto.request.CreateStudioRunRequest;
import com.sqljudge.problem.dto.response.StudioRunOutcome;
import com.sqljudge.problem.dto.response.StudioRunResponse;
import com.sqljudge.problem.entity.DatasetVersion;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.StudioRun;
import com.sqljudge.problem.event.StudioRunCompletedEvent;
import com.sqljudge.problem.event.StudioRunRequestedEvent;
import com.sqljudge.problem.event.StudioTopics;
import com.sqljudge.problem.repository.StudioRunRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudioRunService {

    private static final int EVENT_VERSION = 1;
    private static final StudioRunOutcome NO_OUTCOME = new StudioRunOutcome(null, null, null);

    private final ProblemAccess problemAccess;
    private final StudioRunRepository studioRunRepository;
    private final OutboxWriter outboxWriter;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public StudioRunResponse create(UUID problemId, CreateStudioRunRequest request) {
        Problem problem = problemAccess.accessible(problemId);
        DatasetVersion datasetVersion = datasetVersionOf(problem);
        StudioRun run =
                studioRunRepository.saveAndFlush(
                        StudioRun.pending(
                                problem.getId(),
                                datasetVersion.getId(),
                                request.dialect(),
                                request.sql()));
        outboxWriter.append(
                StudioTopics.RUN_REQUESTED,
                run.getId().toString(),
                new StudioRunRequestedEvent(
                        UuidV7.generate(),
                        clock.instant(),
                        EVENT_VERSION,
                        run.getId(),
                        datasetVersion.getId(),
                        datasetVersion.getChecksum(),
                        run.getDialect(),
                        run.getSqlCode(),
                        problem.getTimeLimitMs()));
        return StudioRunResponse.of(run, NO_OUTCOME);
    }

    @Transactional(readOnly = true)
    public StudioRunResponse get(UUID problemId, UUID runId) {
        Problem problem = problemAccess.accessible(problemId);
        StudioRun run =
                studioRunRepository
                        .findByIdAndProblemId(runId, problem.getId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.STUDIO_RUN_NOT_FOUND,
                                                "Không tìm thấy lần chạy thử"));
        return StudioRunResponse.of(run, outcomeOf(run));
    }

    @Transactional
    public void complete(StudioRunCompletedEvent event) {
        studioRunRepository
                .findById(event.runId())
                .filter(run -> !run.isCompleted())
                .ifPresentOrElse(
                        run -> run.complete(toJson(event.outcome()), clock.instant()),
                        () -> log.info("studio_run_completion_skipped runId={}", event.runId()));
    }

    private static DatasetVersion datasetVersionOf(Problem problem) {
        ProblemRevision draft = problem.getDraftRevision();
        if (draft == null || draft.getDatasetVersion() == null) {
            throw new BusinessRuleException(
                    ErrorCode.PROBLEM_DATASET_MISSING, "Bài chưa chọn database");
        }
        return draft.getDatasetVersion();
    }

    private StudioRunOutcome outcomeOf(StudioRun run) {
        if (run.getResultJson() == null) {
            return NO_OUTCOME;
        }
        try {
            return objectMapper.readValue(run.getResultJson(), StudioRunOutcome.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored studio run result is unreadable", exception);
        }
    }

    private String toJson(StudioRunOutcome outcome) {
        try {
            return objectMapper.writeValueAsString(outcome);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize studio run result", exception);
        }
    }
}
