package com.sqljudge.submission.controller;

import com.sqljudge.submission.dto.request.StartRunRequest;
import com.sqljudge.submission.dto.response.CodeRunResponse;
import com.sqljudge.submission.dto.response.RunAcceptedResponse;
import com.sqljudge.submission.service.CodeRunService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CodeRunController {

    private final CodeRunService codeRunService;

    @PostMapping("/api/problems/{problemId}/runs")
    @Operation(summary = "Chạy thử code trên dataset mẫu của bài, không tính là lần nộp")
    @ApiResponse(responseCode = "202", description = "Đã nhận yêu cầu chạy thử")
    @ApiResponse(
            responseCode = "400",
            description = "Code trống, quá 64 KB hoặc dialect không hỗ trợ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "404", description = "Không có bài hoặc bài chưa xuất bản")
    @ApiResponse(responseCode = "503", description = "Chưa liên lạc được dịch vụ bài tập")
    public ResponseEntity<RunAcceptedResponse> start(
            @PathVariable UUID problemId, @Valid @RequestBody StartRunRequest request) {
        return ResponseEntity.accepted().body(codeRunService.start(problemId, request));
    }

    @GetMapping("/api/runs/{runId}")
    @Operation(summary = "Trạng thái và kết quả một lần chạy thử của chính mình")
    @ApiResponse(responseCode = "200", description = "Lần chạy thử của người gọi")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(
            responseCode = "404",
            description = "Không có lần chạy thử hoặc không phải của người gọi")
    public CodeRunResponse get(@PathVariable UUID runId) {
        return codeRunService.get(runId);
    }
}
