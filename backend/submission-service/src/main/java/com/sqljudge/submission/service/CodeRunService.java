package com.sqljudge.submission.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.submission.client.JudgingInfo;
import com.sqljudge.submission.client.ProblemServiceClient;
import com.sqljudge.submission.dto.request.StartRunRequest;
import com.sqljudge.submission.dto.response.CodeRunResponse;
import com.sqljudge.submission.dto.response.RunAcceptedResponse;
import com.sqljudge.submission.entity.CodeRun;
import com.sqljudge.submission.entity.CodeRun.RunCompletion;
import com.sqljudge.submission.repository.CodeRunRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeRunService {

    private final ProblemServiceClient problemServiceClient;
    private final CodeRunRepository codeRunRepository;
    private final CodeRunWriter codeRunWriter;
    private final Clock clock;

    public RunAcceptedResponse start(UUID problemId, StartRunRequest request) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        JudgingInfo info = problemServiceClient.judgingInfo(problemId);
        if (!info.dialects().contains(request.dialect())) {
            throw new InvalidFieldException("dialect", "Bài này không hỗ trợ dialect đã chọn");
        }
        CodeRun run =
                codeRunWriter.create(
                        CodeRun.pending(
                                userId,
                                info.problemId(),
                                info.revisionId(),
                                request.dialect(),
                                request.code()));
        log.info(
                "code_run_requested runId={} problemId={} codeLength={}",
                run.getId(),
                problemId,
                request.code().length());
        return new RunAcceptedResponse(run.getId());
    }

    @Transactional(readOnly = true)
    public CodeRunResponse get(UUID runId) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        return codeRunRepository
                .findByIdAndUserId(runId, userId)
                .map(CodeRunResponse::from)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.CODE_RUN_NOT_FOUND,
                                        "Không tìm thấy lần chạy thử"));
    }

    @Transactional
    public void complete(UUID runId, RunCompletion completion) {
        codeRunRepository
                .findById(runId)
                .ifPresentOrElse(
                        run -> run.complete(completion, clock.instant()),
                        () -> log.warn("code_run_result_unknown_run runId={}", runId));
    }
}
