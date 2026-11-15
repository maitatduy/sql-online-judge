package com.sqljudge.learning.controller;

import com.sqljudge.learning.dto.response.LessonResponse;
import com.sqljudge.learning.service.LessonQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses/{courseSlug}/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonQueryService lessonQueryService;

    @GetMapping("/{lessonSlug}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Nội dung một bài giảng của khoá đã xuất bản")
    @ApiResponse(responseCode = "200", description = "Bài giảng kèm tên chương và khoá")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "404", description = "Bài hoặc khoá không công khai")
    public LessonResponse getLesson(
            @PathVariable String courseSlug, @PathVariable String lessonSlug) {
        return lessonQueryService.getPublished(courseSlug, lessonSlug);
    }
}
