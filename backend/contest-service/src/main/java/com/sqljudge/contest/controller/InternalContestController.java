package com.sqljudge.contest.controller;

import com.sqljudge.contest.dto.response.ContestEntryResponse;
import com.sqljudge.contest.dto.response.ProblemUsageResponse;
import com.sqljudge.contest.service.ContestEntryService;
import com.sqljudge.contest.service.ContestProblemUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/contests")
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
@RequiredArgsConstructor
public class InternalContestController {

    private final ContestEntryService contestEntryService;
    private final ContestProblemUsageService contestProblemUsageService;

    @GetMapping("/{slug}/problems/{letter}/entry")
    @Operation(summary = "Người dùng có được nộp bài này trong contest lúc này không")
    @ApiResponse(responseCode = "200", description = "Được nộp, kèm id contest và id bài")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    @ApiResponse(responseCode = "403", description = "Contest chưa bắt đầu hoặc chưa đăng ký")
    @ApiResponse(responseCode = "404", description = "Không có contest hoặc không có bài này")
    @ApiResponse(responseCode = "422", description = "Contest đã kết thúc")
    public ContestEntryResponse entry(
            @PathVariable String slug, @PathVariable String letter, @RequestParam UUID userId) {
        return contestEntryService.entry(slug, letter, userId);
    }

    @GetMapping("/problem-usage/{problemId}")
    @Operation(summary = "Bài có đang thuộc một contest chưa kết thúc không")
    @ApiResponse(responseCode = "200", description = "Trạng thái sử dụng của bài")
    @ApiResponse(responseCode = "401", description = "Thiếu hoặc sai token nội bộ")
    public ProblemUsageResponse problemUsage(@PathVariable UUID problemId) {
        return contestProblemUsageService.usageOf(problemId);
    }
}
