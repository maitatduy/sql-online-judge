package com.sqljudge.submission.controller;

import com.sqljudge.submission.dto.response.PublicSubmissionPageResponse;
import com.sqljudge.submission.dto.response.UserActivityResponse;
import com.sqljudge.submission.service.SubmissionStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions/users/{userId}")
@PreAuthorize("permitAll()")
@RequiredArgsConstructor
public class UserSubmissionController {

    private static final int MAX_PAGE_SIZE = 20;

    private final SubmissionStatsService submissionStatsService;

    @GetMapping
    @Operation(
            summary =
                    "Bài nộp luyện tập đã chấm của một người dùng, mới nhất trước, không kèm code")
    @ApiResponse(responseCode = "200", description = "Một trang bài nộp và cursor trang sau")
    @ApiResponse(responseCode = "400", description = "Cursor hoặc kích thước không hợp lệ")
    public PublicSubmissionPageResponse submissions(
            @PathVariable UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "5")
                    @Min(value = 1, message = "Phải từ 1 đến 20") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 20") int size) {
        return submissionStatsService.userSubmissions(userId, cursor, size);
    }

    @GetMapping("/activity")
    @Operation(summary = "Số bài nộp theo ngày giờ Việt Nam của một người dùng trong năm qua")
    @ApiResponse(responseCode = "200", description = "Tổng số bài nộp và các ngày có bài nộp")
    public UserActivityResponse activity(@PathVariable UUID userId) {
        return submissionStatsService.userActivity(userId);
    }
}
