package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.response.InternalJudgingInfoResponse;
import com.sqljudge.problem.service.InternalJudgingInfoService;
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
public class InternalProblemController {

    private final InternalJudgingInfoService internalJudgingInfoService;

    @GetMapping("/{problemId}/judging-info")
    @Operation(summary = "Thông tin để nhận bài nộp: bản đã xuất bản, dialect, số testcase")
    @ApiResponse(responseCode = "200", description = "Bài đã xuất bản và công khai")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    @ApiResponse(responseCode = "404", description = "Không có bài hoặc bài chưa xuất bản")
    public InternalJudgingInfoResponse judgingInfo(@PathVariable UUID problemId) {
        return internalJudgingInfoService.judgingInfo(problemId);
    }
}
