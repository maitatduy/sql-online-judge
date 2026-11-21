package com.sqljudge.learning.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.learning.dto.response.AdminCourseItem;
import com.sqljudge.learning.dto.response.StudioCourseResponse;
import com.sqljudge.learning.entity.CourseStatus;
import com.sqljudge.learning.service.AdminCourseCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/courses")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCourseCatalogController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminCourseCatalogService adminCourseCatalogService;

    @GetMapping("/catalog")
    @Operation(summary = "Mọi khoá chưa xoá, lọc theo trạng thái, cập nhật gần nhất lên đầu")
    @ApiResponse(responseCode = "200", description = "Trang khoá học")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    public PageResponse<AdminCourseItem> catalog(
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return adminCourseCatalogService.catalog(status, page, size);
    }

    @PostMapping("/{courseId}/archive")
    @Operation(summary = "Ẩn khoá đang xuất bản khỏi trang học")
    @ApiResponse(responseCode = "200", description = "Khoá đã ẩn")
    @ApiResponse(responseCode = "422", description = "Khoá không ở trạng thái xuất bản")
    public StudioCourseResponse archive(@PathVariable UUID courseId) {
        return adminCourseCatalogService.archive(courseId);
    }

    @PostMapping("/{courseId}/restore")
    @Operation(summary = "Hiện lại khoá đã ẩn")
    @ApiResponse(responseCode = "200", description = "Khoá xuất bản lại")
    @ApiResponse(responseCode = "422", description = "Khoá chưa bị ẩn")
    public StudioCourseResponse restore(@PathVariable UUID courseId) {
        return adminCourseCatalogService.restore(courseId);
    }
}
