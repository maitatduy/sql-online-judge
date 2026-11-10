package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.request.ReviewProblemRequest;
import com.sqljudge.problem.dto.response.StudioProblemResponse;
import com.sqljudge.problem.service.AdminProblemReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/problems/{problemId}")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProblemReviewController {

    private final AdminProblemReviewService adminProblemReviewService;

    @PostMapping("/approve")
    @Operation(summary = "Duyệt bài đang chờ: chốt bản hiện tại, cấp số thứ tự và xuất bản")
    @ApiResponse(responseCode = "200", description = "Bài đã xuất bản")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có bài")
    @ApiResponse(
            responseCode = "422",
            description = "Bài không chờ duyệt, hoặc đáp án chưa đạt kiểm tra với bản hiện tại")
    public StudioProblemResponse approve(@PathVariable UUID problemId) {
        return adminProblemReviewService.approve(problemId);
    }

    @PostMapping("/request-changes")
    @Operation(summary = "Trả bài về cho người ra đề sửa, kèm lý do")
    @ApiResponse(responseCode = "200", description = "Bài chuyển sang cần sửa")
    @ApiResponse(responseCode = "400", description = "Thiếu lý do hoặc lý do quá dài")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có bài")
    @ApiResponse(responseCode = "422", description = "Bài không ở trạng thái chờ duyệt")
    public StudioProblemResponse requestChanges(
            @PathVariable UUID problemId, @Valid @RequestBody ReviewProblemRequest request) {
        return adminProblemReviewService.requestChanges(problemId, request);
    }

    @PostMapping("/reject")
    @Operation(summary = "Từ chối bài, kèm lý do; bài bị từ chối không gửi duyệt lại được")
    @ApiResponse(responseCode = "200", description = "Bài đã bị từ chối")
    @ApiResponse(responseCode = "400", description = "Thiếu lý do hoặc lý do quá dài")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có bài")
    @ApiResponse(responseCode = "422", description = "Bài không ở trạng thái chờ duyệt")
    public StudioProblemResponse reject(
            @PathVariable UUID problemId, @Valid @RequestBody ReviewProblemRequest request) {
        return adminProblemReviewService.reject(problemId, request);
    }
}
