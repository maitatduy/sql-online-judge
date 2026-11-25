package com.sqljudge.submission.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.submission.dto.response.AdminSubmissionDetailResponse;
import com.sqljudge.submission.dto.response.AdminSubmissionResponse;
import com.sqljudge.submission.entity.Dialect;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.entity.Verdict;
import com.sqljudge.submission.repository.AdminSubmissionFilter;
import com.sqljudge.submission.repository.SubmissionSource;
import com.sqljudge.submission.service.AdminSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/submissions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSubmissionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminSubmissionService adminSubmissionService;

    @GetMapping
    @Operation(summary = "Mọi bài nộp, mới nhất trước, lọc theo verdict, trạng thái, nguồn, bài")
    @ApiResponse(responseCode = "200", description = "Một trang bài nộp, không kèm code")
    @ApiResponse(responseCode = "400", description = "Tham số lọc hoặc phân trang sai")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    public PageResponse<AdminSubmissionResponse> search(
            @RequestParam(required = false) Verdict verdict,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) Dialect dialect,
            @RequestParam(required = false) SubmissionSource source,
            @RequestParam(required = false) @Size(max = 50, message = "Tối đa 50 ký tự") String username,
            @RequestParam(required = false) @Size(max = 160, message = "Tối đa 160 ký tự") String problem,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        AdminSubmissionFilter filter =
                new AdminSubmissionFilter(
                        verdict, status, dialect, source, null, blankToNull(problem));
        return adminSubmissionService.search(filter, username, page, size);
    }

    @GetMapping("/{submissionId}")
    @Operation(summary = "Chi tiết một bài nộp kèm code và kết quả chấm")
    @ApiResponse(responseCode = "200", description = "Bài nộp")
    @ApiResponse(responseCode = "404", description = "Không có bài nộp hoặc đã xoá")
    public AdminSubmissionDetailResponse detail(@PathVariable UUID submissionId) {
        return adminSubmissionService.detail(submissionId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
