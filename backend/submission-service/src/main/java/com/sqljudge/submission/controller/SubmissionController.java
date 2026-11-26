package com.sqljudge.submission.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.submission.dto.request.SubmitSolutionRequest;
import com.sqljudge.submission.dto.response.SubmissionAcceptedResponse;
import com.sqljudge.submission.dto.response.SubmissionCodeResponse;
import com.sqljudge.submission.dto.response.SubmissionResponse;
import com.sqljudge.submission.dto.response.SubmissionSummaryResponse;
import com.sqljudge.submission.service.SubmissionQueryService;
import com.sqljudge.submission.service.SubmissionService;
import com.sqljudge.submission.service.SubmissionService.ContestProblemRef;
import com.sqljudge.submission.service.SubmissionStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SubmissionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SubmissionService submissionService;
    private final SubmissionQueryService submissionQueryService;
    private final SubmissionStreamService submissionStreamService;

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

    @PostMapping("/api/contests/{slug}/problems/{letter}/submissions")
    @Operation(summary = "Nộp lời giải cho một bài trong contest đang diễn ra")
    @ApiResponse(responseCode = "202", description = "Đã nhận bài nộp, đang chờ chấm")
    @ApiResponse(responseCode = "400", description = "Code trống, quá 64 KB hoặc dialect sai")
    @ApiResponse(responseCode = "403", description = "Contest chưa bắt đầu hoặc chưa đăng ký")
    @ApiResponse(responseCode = "404", description = "Không có contest hoặc không có bài này")
    @ApiResponse(responseCode = "422", description = "Contest đã kết thúc")
    @ApiResponse(responseCode = "503", description = "Chưa liên lạc được dịch vụ liên quan")
    public ResponseEntity<SubmissionAcceptedResponse> submitInContest(
            @PathVariable String slug,
            @PathVariable String letter,
            @Valid @RequestBody SubmitSolutionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.accepted()
                .body(
                        submissionService.submitInContest(
                                new ContestProblemRef(slug, letter), request, idempotencyKey));
    }

    @GetMapping("/api/problems/{problemId}/submissions")
    @Operation(summary = "Lịch sử bài nộp của chính mình cho một bài, mới nhất trước")
    @ApiResponse(responseCode = "200", description = "Một trang lịch sử kèm tổng số bài nộp")
    @ApiResponse(responseCode = "400", description = "Số trang hoặc kích thước trang sai")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    public PageResponse<SubmissionSummaryResponse> history(
            @PathVariable UUID problemId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return submissionQueryService.history(problemId, page, size);
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

    @GetMapping("/api/submissions/{submissionId}/code")
    @Operation(summary = "Mã và dialect của một bài nộp của chính mình")
    @ApiResponse(responseCode = "200", description = "Mã đã nộp")
    @ApiResponse(responseCode = "400", description = "Id bài nộp không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(
            responseCode = "404",
            description = "Không có bài nộp hoặc không phải của người gọi")
    public SubmissionCodeResponse getSubmissionCode(@PathVariable UUID submissionId) {
        return submissionQueryService.code(submissionId);
    }

    @GetMapping(
            path = "/api/submissions/{submissionId}/events",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary =
                    "Luồng SSE trạng thái bài nộp của chính mình, đóng lại khi chấm xong;"
                            + " mỗi sự kiện tên submission mang cùng dữ liệu với GET bài nộp")
    @ApiResponse(responseCode = "200", description = "Luồng sự kiện của bài nộp")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(
            responseCode = "404",
            description = "Không có bài nộp hoặc không phải của người gọi")
    public SseEmitter streamSubmission(@PathVariable UUID submissionId) {
        return submissionStreamService.open(submissionId);
    }
}
