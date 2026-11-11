package com.sqljudge.submission.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.submission.dto.response.SubmissionResponse;
import com.sqljudge.submission.repository.SubmissionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmissionQueryService {

    private final SubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public SubmissionResponse get(UUID submissionId) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        return submissionRepository
                .findByIdAndUserIdAndDeletedAtIsNull(submissionId, userId)
                .map(SubmissionResponse::from)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.SUBMISSION_NOT_FOUND, "Không tìm thấy bài nộp"));
    }
}
