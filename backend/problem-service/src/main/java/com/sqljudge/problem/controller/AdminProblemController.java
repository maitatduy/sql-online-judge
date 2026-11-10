package com.sqljudge.problem.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.problem.dto.response.AdminProblemSummaryResponse;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.service.AdminProblemQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/problems")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProblemController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminProblemQueryService adminProblemQueryService;

    @GetMapping
    @Operation(summary = "Bài theo trạng thái cho admin, mặc định bài chờ duyệt, cũ nhất lên đầu")
    @ApiResponse(responseCode = "200", description = "Một trang bài")
    @ApiResponse(responseCode = "400", description = "Trạng thái hoặc tham số phân trang sai")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    public PageResponse<AdminProblemSummaryResponse> listProblems(
            @RequestParam(defaultValue = "PENDING_REVIEW") ProblemStatus status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return adminProblemQueryService.list(status, page, size);
    }
}
