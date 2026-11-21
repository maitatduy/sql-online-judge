package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.response.InternalContestProblemResponse;
import com.sqljudge.problem.dto.response.InternalJudgingInfoResponse;
import com.sqljudge.problem.service.InternalContestProblemService;
import com.sqljudge.problem.service.InternalJudgingInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/problems")
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
@RequiredArgsConstructor
public class InternalProblemController {

    private static final int MAX_CONTEST_PROBLEMS = 26;

    private final InternalJudgingInfoService internalJudgingInfoService;
    private final InternalContestProblemService internalContestProblemService;

    @GetMapping("/contest-ready")
    @Operation(summary = "Bài đã duyệt cho contest trong danh sách id, bỏ qua id không hợp lệ")
    @ApiResponse(responseCode = "200", description = "Các bài dùng được cho contest")
    @ApiResponse(responseCode = "400", description = "Quá 26 id")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    public List<InternalContestProblemResponse> contestReady(
            @RequestParam @Size(max = MAX_CONTEST_PROBLEMS, message = "Tối đa 26 bài") List<UUID> ids) {
        return internalContestProblemService.contestReady(ids);
    }

    @GetMapping("/{problemId}/judging-info")
    @Operation(summary = "Thông tin để nhận bài nộp: bản đã xuất bản, dialect, số testcase")
    @ApiResponse(responseCode = "200", description = "Bài đã xuất bản và công khai")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    @ApiResponse(responseCode = "404", description = "Không có bài hoặc bài chưa xuất bản")
    public InternalJudgingInfoResponse judgingInfo(@PathVariable UUID problemId) {
        return internalJudgingInfoService.judgingInfo(problemId);
    }
}
