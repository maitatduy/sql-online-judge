package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.request.SaveProblemAnswerRequest;
import com.sqljudge.problem.dto.response.ProblemAnswerResponse;
import com.sqljudge.problem.service.StudioAnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studio/problems/{problemId}/answer")
@PreAuthorize("hasAnyRole('SETTER', 'ADMIN')")
@RequiredArgsConstructor
public class StudioAnswerController {

    private final StudioAnswerService studioAnswerService;

    @GetMapping
    @Operation(summary = "Đáp án chuẩn, testcase và cài đặt so khớp của bản nháp")
    @ApiResponse(responseCode = "200", description = "Phần đáp án hiện tại, có thể còn trống")
    @ApiResponse(
            responseCode = "404",
            description = "Không có bài hoặc không phải bài của người gọi")
    public ProblemAnswerResponse getAnswer(@PathVariable UUID problemId) {
        return studioAnswerService.get(problemId);
    }

    @PutMapping
    @Operation(summary = "Lưu đáp án chuẩn theo dialect, testcase và cài đặt so khớp")
    @ApiResponse(responseCode = "200", description = "Phần đáp án sau khi lưu")
    @ApiResponse(
            responseCode = "400",
            description = "Thiếu đáp án PostgreSQL, trùng dialect hoặc bộ dữ liệu không hợp lệ")
    @ApiResponse(
            responseCode = "404",
            description = "Không có bài hoặc không phải bài của người gọi")
    @ApiResponse(responseCode = "422", description = "Bài chưa chọn database hoặc đã khoá")
    public ProblemAnswerResponse saveAnswer(
            @PathVariable UUID problemId, @Valid @RequestBody SaveProblemAnswerRequest request) {
        return studioAnswerService.save(problemId, request);
    }
}
