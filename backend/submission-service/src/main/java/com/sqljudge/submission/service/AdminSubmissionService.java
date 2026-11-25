package com.sqljudge.submission.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.web.PageResponse;
import com.sqljudge.submission.client.AuthServiceClient;
import com.sqljudge.submission.client.UserName;
import com.sqljudge.submission.dto.response.AdminSubmissionDetailResponse;
import com.sqljudge.submission.dto.response.AdminSubmissionResponse;
import com.sqljudge.submission.entity.Submission;
import com.sqljudge.submission.repository.AdminSubmissionFilter;
import com.sqljudge.submission.repository.AdminSubmissionRepository;
import com.sqljudge.submission.repository.AdminSubmissionRow;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminSubmissionService {

    private final AdminSubmissionRepository adminSubmissionRepository;
    private final AuthServiceClient authServiceClient;

    public PageResponse<AdminSubmissionResponse> search(
            AdminSubmissionFilter filter, String username, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Optional<AdminSubmissionFilter> resolved = withUser(filter, username);
        if (resolved.isEmpty()) {
            return PageResponse.from(
                    Page.<AdminSubmissionResponse>empty(pageRequest), Function.identity());
        }
        Page<AdminSubmissionRow> rows =
                adminSubmissionRepository.search(resolved.get(), pageRequest);
        Map<UUID, UserName> names = namesOf(rows.map(AdminSubmissionRow::userId).toList());
        return PageResponse.from(
                rows, row -> AdminSubmissionResponse.of(row, names.get(row.userId())));
    }

    public AdminSubmissionDetailResponse detail(UUID submissionId) {
        Submission submission =
                adminSubmissionRepository
                        .findById(submissionId)
                        .filter(found -> found.getDeletedAt() == null)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.SUBMISSION_NOT_FOUND,
                                                "Không tìm thấy bài nộp"));
        return AdminSubmissionDetailResponse.of(
                submission, namesOf(List.of(submission.getUserId())).get(submission.getUserId()));
    }

    private Optional<AdminSubmissionFilter> withUser(
            AdminSubmissionFilter filter, String username) {
        if (username == null || username.isBlank()) {
            return Optional.of(filter);
        }
        return authServiceClient
                .findByUsername(username.trim())
                .id()
                .map(
                        userId ->
                                new AdminSubmissionFilter(
                                        filter.verdict(),
                                        filter.status(),
                                        filter.dialect(),
                                        filter.source(),
                                        userId,
                                        filter.problemSlug()));
    }

    private Map<UUID, UserName> namesOf(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return authServiceClient.userNames(userIds.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(UserName::userId, Function.identity(), (a, b) -> a));
    }
}
