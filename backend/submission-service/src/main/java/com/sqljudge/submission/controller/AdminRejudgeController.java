package com.sqljudge.submission.controller;

import com.sqljudge.submission.dto.response.RejudgeBatchResponse;
import com.sqljudge.submission.service.RejudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/problems/{problemId}/rejudge")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRejudgeController {

    private final RejudgeService rejudgeService;

    @PostMapping
    @Operation(summary = "Chấm lại mọi bài nộp luyện tập của bài theo phiên bản đang xuất bản")
    @ApiResponse(responseCode = "202", description = "Đã đưa các bài nộp vào hàng đợi chấm lại")
    @ApiResponse(responseCode = "404", description = "Không có bài đã xuất bản")
    @ApiResponse(responseCode = "409", description = "Bài đang được chấm lại")
    public ResponseEntity<RejudgeBatchResponse> start(@PathVariable UUID problemId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(rejudgeService.start(problemId));
    }

    @GetMapping
    @Operation(summary = "Tiến độ đợt chấm lại gần nhất của bài")
    @ApiResponse(responseCode = "200", description = "Tiến độ đợt chấm lại")
    @ApiResponse(responseCode = "404", description = "Bài chưa được chấm lại")
    public RejudgeBatchResponse latest(@PathVariable UUID problemId) {
        return rejudgeService.latest(problemId);
    }
}
