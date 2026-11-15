package com.sqljudge.learning.controller;

import com.sqljudge.learning.dto.response.CourseProgressResponse;
import com.sqljudge.learning.service.LearningProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses/{courseSlug}")
@RequiredArgsConstructor
public class LearningProgressController {

    private final LearningProgressService learningProgressService;

    @GetMapping("/progress")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tiến độ của người đang đăng nhập trong một khoá học")
    @ApiResponse(responseCode = "200", description = "Bài đã hoàn thành và bài tiếp theo")
    @ApiResponse(responseCode = "404", description = "Khoá chưa xuất bản hoặc không tồn tại")
    public CourseProgressResponse getProgress(@PathVariable String courseSlug) {
        return learningProgressService.progress(courseSlug);
    }

    @PostMapping("/enrollment")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đăng ký học một khoá, gọi lại nhiều lần không tạo bản trùng")
    @ApiResponse(responseCode = "200", description = "Tiến độ sau khi đăng ký")
    @ApiResponse(responseCode = "404", description = "Khoá chưa xuất bản hoặc không tồn tại")
    public CourseProgressResponse enroll(@PathVariable String courseSlug) {
        return learningProgressService.enroll(courseSlug);
    }

    @PostMapping("/lessons/{lessonSlug}/completion")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đánh dấu hoàn thành bài giảng, tự đăng ký khoá nếu chưa đăng ký")
    @ApiResponse(responseCode = "200", description = "Tiến độ sau khi hoàn thành")
    @ApiResponse(responseCode = "404", description = "Khoá hoặc bài không công khai")
    public CourseProgressResponse completeLesson(
            @PathVariable String courseSlug, @PathVariable String lessonSlug) {
        return learningProgressService.complete(courseSlug, lessonSlug);
    }
}
