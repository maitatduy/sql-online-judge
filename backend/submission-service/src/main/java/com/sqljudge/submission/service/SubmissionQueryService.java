package com.sqljudge.submission.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.common.web.PageResponse;
import com.sqljudge.submission.dto.response.SubmissionCodeResponse;
import com.sqljudge.submission.dto.response.SubmissionResponse;
import com.sqljudge.submission.dto.response.SubmissionSummaryResponse;
import com.sqljudge.submission.entity.Submission;
import com.sqljudge.submission.repository.SubmissionRepository;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmissionQueryService {

    private final SubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public SubmissionResponse get(UUID submissionId) {
        return SubmissionResponse.from(ownSubmission(submissionId));
    }

    @Transactional(readOnly = true)
    public SubmissionCodeResponse code(UUID submissionId) {
        return SubmissionCodeResponse.from(ownSubmission(submissionId));
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionSummaryResponse> history(UUID problemId, int page, int size) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        return PageResponse.from(
                submissionRepository.findHistory(userId, problemId, PageRequest.of(page - 1, size)),
                Function.identity());
    }

    private Submission ownSubmission(UUID submissionId) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        return submissionRepository
                .findByIdAndUserIdAndDeletedAtIsNull(submissionId, userId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.SUBMISSION_NOT_FOUND, "Không tìm thấy bài nộp"));
    }
}
