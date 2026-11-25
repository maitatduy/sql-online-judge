package com.sqljudge.submission.controller;

import com.sqljudge.submission.dto.response.JudgeAttentionResponse;
import com.sqljudge.submission.dto.response.JudgeQueueResponse;
import com.sqljudge.submission.service.JudgeQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminJudgeController {

    private final JudgeQueueService judgeQueueService;

    @GetMapping("/api/admin/judge/queue")
    @Operation(summary = "Số bài đang chờ, đang chấm, bị kẹt, lỗi hệ thống chấm và đã chấm")
    @ApiResponse(responseCode = "200", description = "Tình trạng hàng đợi chấm")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    public JudgeQueueResponse queue() {
        return judgeQueueService.queue();
    }

    @GetMapping("/api/admin/judge/attention")
    @Operation(summary = "Bài bị kẹt trong hàng đợi và bài lỗi hệ thống chấm 7 ngày qua")
    @ApiResponse(responseCode = "200", description = "Tối đa 50 bài, mới nhất trước")
    public JudgeAttentionResponse attention() {
        return judgeQueueService.attention();
    }

    @PostMapping("/api/admin/submissions/{submissionId}/rejudge")
    @Operation(summary = "Chấm lại một bài lỗi hệ thống chấm hoặc bị kẹt, trên phiên bản đã nộp")
    @ApiResponse(responseCode = "202", description = "Đã đưa lại vào hàng đợi chấm")
    @ApiResponse(responseCode = "404", description = "Không có bài nộp hoặc đã xoá")
    @ApiResponse(responseCode = "422", description = "Bài đã chấm xong hoặc đang chấm bình thường")
    public ResponseEntity<Void> rejudge(@PathVariable UUID submissionId) {
        judgeQueueService.rejudge(submissionId);
        return ResponseEntity.accepted().build();
    }
}
