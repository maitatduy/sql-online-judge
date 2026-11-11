package com.sqljudge.submission.controller;

import com.sqljudge.submission.dto.request.SubmitSolutionRequest;
import com.sqljudge.submission.dto.response.SubmissionAcceptedResponse;
import com.sqljudge.submission.dto.response.SubmissionResponse;
import com.sqljudge.submission.service.SubmissionQueryService;
import com.sqljudge.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SubmissionQueryService submissionQueryService;

    @PostMapping("/api/problems/{problemId}/submissions")
    @Operation(summary = "Nộp lời giải cho bài đã xuất bản; kết quả chấm đến sau")
    @ApiResponse(responseCode = "202", description = "Đã nhận bài nộp, đang chờ chấm")
    @ApiResponse(
            responseCode = "400",
            description = "Code trống, quá 64 KB hoặc dialect không hỗ trợ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "404", description = "Không có bài hoặc bài chưa xuất bản")
    @ApiResponse(responseCode = "503", description = "Chưa liên lạc được dịch vụ bài tập")
    public ResponseEntity<SubmissionAcceptedResponse> submit(
            @PathVariable UUID problemId,
            @Valid @RequestBody SubmitSolutionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.accepted()
                .body(submissionService.submit(problemId, request, idempotencyKey));
    }

    @GetMapping("/api/submissions/{submissionId}")
    @Operation(summary = "Trạng thái, tiến độ và kết quả chấm một bài nộp của chính mình")
    @ApiResponse(responseCode = "200", description = "Bài nộp của người gọi")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(
            responseCode = "404",
            description = "Không có bài nộp hoặc không phải của người gọi")
    public SubmissionResponse getSubmission(@PathVariable UUID submissionId) {
        return submissionQueryService.get(submissionId);
    }
}
