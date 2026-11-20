package com.sqljudge.learning.controller;

import com.sqljudge.learning.dto.request.QuizQuestionRequest;
import com.sqljudge.learning.dto.response.StudioQuizQuestionResponse;
import com.sqljudge.learning.service.StudioQuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('SETTER', 'ADMIN')")
@RequiredArgsConstructor
public class StudioQuizController {

    private final StudioQuizService studioQuizService;

    @GetMapping("/api/studio/lessons/{lessonId}/quiz-questions")
    @Operation(summary = "Câu hỏi quiz của bài giảng đang soạn, kèm đáp án đúng")
    @ApiResponse(responseCode = "200", description = "Câu hỏi theo thứ tự")
    @ApiResponse(responseCode = "404", description = "Không có bài hoặc không phải của người gọi")
    public List<StudioQuizQuestionResponse> list(@PathVariable UUID lessonId) {
        return studioQuizService.list(lessonId);
    }

    @PostMapping("/api/studio/lessons/{lessonId}/quiz-questions")
    @Operation(summary = "Thêm câu hỏi quiz vào cuối bài")
    @ApiResponse(responseCode = "201", description = "Câu hỏi vừa thêm")
    @ApiResponse(responseCode = "400", description = "Thiếu lựa chọn hoặc đáp án đúng sai luật")
    @ApiResponse(responseCode = "422", description = "Khoá đang chờ duyệt")
    public ResponseEntity<StudioQuizQuestionResponse> add(
            @PathVariable UUID lessonId, @Valid @RequestBody QuizQuestionRequest request) {
        StudioQuizQuestionResponse question = studioQuizService.add(lessonId, request);
        return ResponseEntity.created(URI.create("/api/studio/quiz-questions/" + question.id()))
                .body(question);
    }

    @PutMapping("/api/studio/quiz-questions/{questionId}")
    @Operation(summary = "Thay đề và toàn bộ lựa chọn của câu hỏi")
    @ApiResponse(responseCode = "200", description = "Câu hỏi sau khi sửa")
    @ApiResponse(responseCode = "400", description = "Thiếu lựa chọn hoặc đáp án đúng sai luật")
    public StudioQuizQuestionResponse replace(
            @PathVariable UUID questionId, @Valid @RequestBody QuizQuestionRequest request) {
        return studioQuizService.replace(questionId, request);
    }

    @DeleteMapping("/api/studio/quiz-questions/{questionId}")
    @Operation(summary = "Xoá câu hỏi cùng các lựa chọn")
    @ApiResponse(responseCode = "204", description = "Đã xoá")
    @ApiResponse(responseCode = "422", description = "Khoá đang chờ duyệt")
    public ResponseEntity<Void> delete(@PathVariable UUID questionId) {
        studioQuizService.delete(questionId);
        return ResponseEntity.noContent().build();
    }
}
