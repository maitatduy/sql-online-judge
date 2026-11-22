package com.sqljudge.submission.service;

import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.submission.client.ContestEntry;
import com.sqljudge.submission.client.ContestServiceClient;
import com.sqljudge.submission.client.JudgingInfo;
import com.sqljudge.submission.client.ProblemServiceClient;
import com.sqljudge.submission.dto.request.SubmitSolutionRequest;
import com.sqljudge.submission.dto.response.SubmissionAcceptedResponse;
import com.sqljudge.submission.entity.Submission;
import com.sqljudge.submission.entity.Submission.ProblemLabel;
import com.sqljudge.submission.entity.Submission.SubmittedSolution;
import com.sqljudge.submission.repository.SubmissionRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private final ProblemServiceClient problemServiceClient;
    private final ContestServiceClient contestServiceClient;
    private final SubmissionRepository submissionRepository;
    private final SubmissionWriter submissionWriter;

    public SubmissionAcceptedResponse submit(
            UUID problemId, SubmitSolutionRequest request, String idempotencyKey) {
        return accept(
                request,
                idempotencyKey,
                () -> new Target(problemServiceClient.judgingInfo(problemId), null));
    }

    public SubmissionAcceptedResponse submitInContest(
            ContestProblemRef problem, SubmitSolutionRequest request, String idempotencyKey) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        return accept(
                request,
                idempotencyKey,
                () -> {
                    ContestEntry entry =
                            contestServiceClient.entry(problem.slug(), problem.letter(), userId);
                    return new Target(
                            problemServiceClient.contestJudgingInfo(entry.problemId()),
                            entry.contestId());
                });
    }

    private SubmissionAcceptedResponse accept(
            SubmitSolutionRequest request, String idempotencyKey, Supplier<Target> resolveTarget) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        if (idempotencyKey != null && idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new InvalidFieldException("Idempotency-Key", "Tối đa 100 ký tự");
        }
        Optional<Submission> existing = existingSubmission(userId, idempotencyKey);
        if (existing.isPresent()) {
            return new SubmissionAcceptedResponse(existing.get().getId());
        }
        Target target = resolveTarget.get();
        JudgingInfo info = target.info();
        if (!info.dialects().contains(request.dialect())) {
            throw new InvalidFieldException("dialect", "Bài này không hỗ trợ dialect đã chọn");
        }
        Submission pending =
                Submission.pending(
                        userId,
                        new SubmittedSolution(
                                info.problemId(),
                                info.revisionId(),
                                new ProblemLabel(info.number(), info.slug(), info.title()),
                                request.dialect(),
                                request.code(),
                                info.testcaseCount(),
                                target.contestId()),
                        idempotencyKey);
        try {
            Submission submission = submissionWriter.create(pending);
            log.info(
                    "submission_created submissionId={} problemId={} contestId={} codeLength={}",
                    submission.getId(),
                    info.problemId(),
                    target.contestId(),
                    request.code().length());
            return new SubmissionAcceptedResponse(submission.getId());
        } catch (DataIntegrityViolationException duplicate) {
            return existingSubmission(userId, idempotencyKey)
                    .map(submission -> new SubmissionAcceptedResponse(submission.getId()))
                    .orElseThrow(() -> duplicate);
        }
    }

    private Optional<Submission> existingSubmission(UUID userId, String idempotencyKey) {
        return idempotencyKey == null
                ? Optional.empty()
                : submissionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
    }

    public record ContestProblemRef(String slug, String letter) {}

    private record Target(JudgingInfo info, UUID contestId) {}
}
