package com.sqljudge.submission.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.submission.client.ProblemServiceClient;
import com.sqljudge.submission.dto.response.JudgingSummaryResponse;
import com.sqljudge.submission.dto.response.RejudgeBatchResponse;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.entity.Verdict;
import com.sqljudge.submission.repository.RejudgeBatchRepository;
import com.sqljudge.submission.repository.SubmissionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RejudgeService {

    private final ProblemServiceClient problemServiceClient;
    private final RejudgeWriter rejudgeWriter;
    private final RejudgeBatchRepository batchRepository;
    private final SubmissionRepository submissionRepository;

    public RejudgeBatchResponse start(UUID problemId) {
        rejudgeWriter.requireNoRunningBatch(problemId);
        return rejudgeWriter.start(problemServiceClient.contestJudgingInfo(problemId));
    }

    @Transactional(readOnly = true)
    public RejudgeBatchResponse latest(UUID problemId) {
        return batchRepository
                .findFirstByProblemIdOrderByCreatedAtDesc(problemId)
                .map(RejudgeBatchResponse::from)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.REJUDGE_NOT_FOUND, "Bài chưa được chấm lại"));
    }

    @Transactional(readOnly = true)
    public JudgingSummaryResponse summary(UUID problemId) {
        SubmissionStatus completed = SubmissionStatus.COMPLETED;
        return new JudgingSummaryResponse(
                submissionRepository.countJudged(problemId, completed, Verdict.SE),
                submissionRepository.countAccepted(problemId, completed, Verdict.SE, Verdict.AC),
                submissionRepository.findUserJudgings(
                        problemId, completed, Verdict.SE, Verdict.AC));
    }
}
