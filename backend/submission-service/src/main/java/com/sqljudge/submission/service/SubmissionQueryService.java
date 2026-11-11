package com.sqljudge.submission.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.submission.dto.response.SubmissionPageResponse;
import com.sqljudge.submission.dto.response.SubmissionResponse;
import com.sqljudge.submission.dto.response.SubmissionSummaryResponse;
import com.sqljudge.submission.repository.SubmissionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
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

    @Transactional(readOnly = true)
    public SubmissionPageResponse history(UUID problemId, String cursor, int size) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        Limit limit = Limit.of(size + 1);
        List<SubmissionSummaryResponse> rows =
                cursor == null
                        ? submissionRepository.findHistory(userId, problemId, limit)
                        : submissionRepository.findHistoryBefore(
                                userId, problemId, SubmissionCursor.decode(cursor), limit);
        if (rows.size() <= size) {
            return new SubmissionPageResponse(rows, null);
        }
        List<SubmissionSummaryResponse> items = rows.subList(0, size);
        return new SubmissionPageResponse(items, SubmissionCursor.encode(items.get(size - 1).id()));
    }
}
