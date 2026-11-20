package com.sqljudge.learning.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.learning.dto.request.ReviewCourseRequest;
import com.sqljudge.learning.dto.response.CourseReviewQueueItem;
import com.sqljudge.learning.dto.response.StudioCourseResponse;
import com.sqljudge.learning.service.AdminCourseReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/courses")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCourseReviewController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminCourseReviewService adminCourseReviewService;

    @GetMapping
    @Operation(summary = "Khoá đang chờ duyệt, gửi sớm nhất lên đầu")
    @ApiResponse(responseCode = "200", description = "Trang khoá chờ duyệt")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    public PageResponse<CourseReviewQueueItem> queue(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return adminCourseReviewService.queue(page, size);
    }

    @PostMapping("/{courseId}/approve")
    @Operation(summary = "Duyệt và xuất bản bản đang chờ duyệt")
    @ApiResponse(responseCode = "200", description = "Khoá đã xuất bản")
    @ApiResponse(responseCode = "422", description = "Khoá không ở trạng thái chờ duyệt")
    public StudioCourseResponse approve(@PathVariable UUID courseId) {
        return adminCourseReviewService.approve(courseId);
    }

    @PostMapping("/{courseId}/request-changes")
    @Operation(summary = "Trả khoá về cho người soạn sửa, kèm lý do")
    @ApiResponse(responseCode = "200", description = "Khoá về bản nháp")
    @ApiResponse(responseCode = "400", description = "Thiếu lý do hoặc lý do quá dài")
    @ApiResponse(responseCode = "422", description = "Khoá không ở trạng thái chờ duyệt")
    public StudioCourseResponse requestChanges(
            @PathVariable UUID courseId, @Valid @RequestBody ReviewCourseRequest request) {
        return adminCourseReviewService.requestChanges(courseId, request.reason());
    }
}
