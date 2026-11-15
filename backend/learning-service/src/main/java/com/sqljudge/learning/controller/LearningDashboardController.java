package com.sqljudge.learning.controller;

import com.sqljudge.learning.dto.response.ActivityDayResponse;
import com.sqljudge.learning.dto.response.EnrolledCourseResponse;
import com.sqljudge.learning.service.LearningDashboardService;
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
public class LearningDashboardController {

    private final LearningDashboardService learningDashboardService;

    @GetMapping("/api/enrollments")
    @Operation(summary = "Các khoá đang học của người gọi, khoá đăng ký gần nhất đứng trước")
    @ApiResponse(responseCode = "200", description = "Khoá đang học kèm tiến độ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    public List<EnrolledCourseResponse> enrollments() {
        return learningDashboardService.enrollments();
    }

    @GetMapping("/api/learning/activity")
    @Operation(summary = "Số bài giảng hoàn thành theo ngày, tính theo giờ Việt Nam")
    @ApiResponse(responseCode = "200", description = "Các ngày có hoạt động, tăng dần")
    @ApiResponse(responseCode = "400", description = "Khoảng thời gian vượt một năm")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    public List<ActivityDayResponse> activity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from) {
        return learningDashboardService.activity(from);
    }
}
