package com.sqljudge.problem.controller;

import com.sqljudge.problem.dto.response.AnswerCheckResponse;
import com.sqljudge.problem.service.AnswerCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studio/problems/{problemId}/answer-checks")
@PreAuthorize("hasAnyRole('SETTER', 'ADMIN')")
@RequiredArgsConstructor
public class AnswerCheckController {

    private final AnswerCheckService answerCheckService;

    @PostMapping
    @Operation(summary = "Chạy đáp án chuẩn trên mọi testcase và mọi dialect, so với PostgreSQL")
    @ApiResponse(responseCode = "202", description = "Đã nhận, kết quả xem qua header Location")
    @ApiResponse(
            responseCode = "404",
            description = "Không có bài hoặc không phải bài của người gọi")
    @ApiResponse(
            responseCode = "422",
            description = "Bài chưa chọn database, chưa có đáp án chuẩn hoặc testcase")
    public ResponseEntity<AnswerCheckResponse> startCheck(@PathVariable UUID problemId) {
        AnswerCheckResponse check = answerCheckService.start(problemId);
        URI location =
                URI.create("/api/studio/problems/" + problemId + "/answer-checks/" + check.id());
        return ResponseEntity.accepted().location(location).body(check);
    }

    @GetMapping("/latest")
    @Operation(summary = "Lần kiểm tra đáp án gần nhất, kèm còn khớp với đáp án hiện tại hay không")
    @ApiResponse(responseCode = "200", description = "Lần kiểm tra gần nhất")
    @ApiResponse(responseCode = "404", description = "Không có bài hoặc bài chưa kiểm tra lần nào")
    public AnswerCheckResponse getLatestCheck(@PathVariable UUID problemId) {
        return answerCheckService.latest(problemId);
    }

    @GetMapping("/{checkId}")
    @Operation(summary = "Trạng thái và kết quả từng testcase của một lần kiểm tra đáp án")
    @ApiResponse(responseCode = "200", description = "Đang chờ hoặc đã có kết quả")
    @ApiResponse(responseCode = "404", description = "Không có bài hoặc lần kiểm tra")
    public AnswerCheckResponse getCheck(@PathVariable UUID problemId, @PathVariable UUID checkId) {
        return answerCheckService.get(problemId, checkId);
    }
}
