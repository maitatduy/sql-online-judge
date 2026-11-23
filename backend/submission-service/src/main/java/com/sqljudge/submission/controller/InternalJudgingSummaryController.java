package com.sqljudge.submission.controller;

import com.sqljudge.submission.dto.response.JudgingSummaryResponse;
import com.sqljudge.submission.service.RejudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/problems")
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
@RequiredArgsConstructor
public class InternalJudgingSummaryController {

    private final RejudgeService rejudgeService;

    @GetMapping("/{problemId}/judging-summary")
    @Operation(summary = "Tổng lượt nộp, lượt AC và lần AC đầu của từng người cho một bài")
    @ApiResponse(responseCode = "200", description = "Số liệu chấm bài của bài")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    public JudgingSummaryResponse judgingSummary(@PathVariable UUID problemId) {
        return rejudgeService.summary(problemId);
    }
}
