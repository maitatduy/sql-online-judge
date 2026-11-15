package com.sqljudge.learning.controller;

import com.sqljudge.learning.dto.request.StartLessonRunRequest;
import com.sqljudge.learning.dto.response.LessonRunAcceptedResponse;
import com.sqljudge.learning.dto.response.LessonRunResponse;
import com.sqljudge.learning.service.LessonRunService;
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
public class LessonRunController {

    private final LessonRunService lessonRunService;

    @PostMapping("/api/lesson-cells/{cellKey}/runs")
    @Operation(summary = "Chạy SQL của một ô bài giảng trên dataset của ô, không chấm điểm")
    @ApiResponse(responseCode = "202", description = "Đã nhận yêu cầu chạy")
    @ApiResponse(responseCode = "400", description = "Câu lệnh trống hoặc quá 64 KB")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "404", description = "Ô không thuộc khoá đã xuất bản")
    public ResponseEntity<LessonRunAcceptedResponse> start(
            @PathVariable UUID cellKey, @Valid @RequestBody StartLessonRunRequest request) {
        return ResponseEntity.accepted().body(lessonRunService.start(cellKey, request));
    }

    @GetMapping("/api/lesson-runs/{runId}")
    @Operation(summary = "Trạng thái và kết quả một lần chạy ô bài giảng của chính mình")
    @ApiResponse(responseCode = "200", description = "Lần chạy của người gọi")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(
            responseCode = "404",
            description = "Không có lần chạy hoặc không phải của người gọi")
    public LessonRunResponse get(@PathVariable UUID runId) {
        return lessonRunService.get(runId);
    }
}
