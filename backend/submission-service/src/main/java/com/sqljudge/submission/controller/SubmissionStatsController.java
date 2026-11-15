package com.sqljudge.submission.controller;

import com.sqljudge.submission.dto.response.ActivityDayResponse;
import com.sqljudge.submission.dto.response.SubmissionStatsResponse;
import com.sqljudge.submission.service.SubmissionStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SubmissionStatsController {

    private final SubmissionStatsService submissionStatsService;

    @GetMapping("/api/submissions/summary")
    @Operation(summary = "Tổng hợp bài nộp của người gọi: số lần nộp, AC, bài đã giải")
    @ApiResponse(responseCode = "200", description = "Số liệu bài nộp")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    public SubmissionStatsResponse summary() {
        return submissionStatsService.summary();
    }

    @GetMapping("/api/submissions/activity")
    @Operation(summary = "Số bài nộp theo ngày của người gọi, tính theo giờ Việt Nam")
    @ApiResponse(responseCode = "200", description = "Các ngày có bài nộp, tăng dần")
    @ApiResponse(responseCode = "400", description = "Khoảng thời gian vượt một năm")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    public List<ActivityDayResponse> activity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from) {
        return submissionStatsService.activity(from);
    }
}
