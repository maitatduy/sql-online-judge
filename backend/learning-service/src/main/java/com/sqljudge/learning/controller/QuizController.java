package com.sqljudge.learning.controller;

import com.sqljudge.learning.dto.request.SubmitQuizRequest;
import com.sqljudge.learning.dto.response.QuizResultResponse;
import com.sqljudge.learning.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/api/courses/{courseSlug}/lessons/{lessonSlug}/quiz-attempts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Nộp câu trả lời quiz, đúng hết thì bài được hoàn thành")
    @ApiResponse(responseCode = "200", description = "Câu nào đúng, câu nào sai và tiến độ mới")
    @ApiResponse(responseCode = "400", description = "Thiếu danh sách câu trả lời")
    @ApiResponse(responseCode = "404", description = "Khoá hoặc bài không công khai")
    @ApiResponse(responseCode = "422", description = "Bài giảng không có câu hỏi")
    public QuizResultResponse submit(
            @PathVariable String courseSlug,
            @PathVariable String lessonSlug,
            @Valid @RequestBody SubmitQuizRequest request) {
        return quizService.grade(courseSlug, lessonSlug, request);
    }
}
