package com.sqljudge.learning.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.learning.dto.response.CourseDetailResponse;
import com.sqljudge.learning.dto.response.CourseSummaryResponse;
import com.sqljudge.learning.entity.Difficulty;
import com.sqljudge.learning.service.CourseQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CourseQueryService courseQueryService;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Danh sách khoá học đã xuất bản, khoá mới xuất bản đứng trước")
    @ApiResponse(responseCode = "200", description = "Một trang khoá học")
    @ApiResponse(responseCode = "400", description = "Tham số phân trang không hợp lệ")
    public PageResponse<CourseSummaryResponse> listCourses(
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return courseQueryService.listPublished(difficulty, page, size);
    }

    @GetMapping("/{slug}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Đề cương khoá học đã xuất bản: chương và bài giảng theo thứ tự")
    @ApiResponse(responseCode = "200", description = "Khoá học kèm đề cương")
    @ApiResponse(responseCode = "404", description = "Khoá chưa xuất bản hoặc không tồn tại")
    public CourseDetailResponse getCourse(@PathVariable String slug) {
        return courseQueryService.getPublished(slug);
    }
}
