package com.sqljudge.problem.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.problem.dto.response.AdminProblemBankSummaryResponse;
import com.sqljudge.problem.dto.response.AdminProblemSummaryResponse;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.service.AdminProblemQueryService;
import com.sqljudge.problem.service.AdminProblemSearch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    private static final int MAX_KEYWORD_LENGTH = 200;

    private final AdminProblemQueryService adminProblemQueryService;

    @GetMapping
    @Operation(
            summary =
                    "Bài cho admin theo trạng thái, phạm vi, độ khó, tag, từ khoá; mặc định bài chờ"
                            + " duyệt, cũ nhất lên đầu")
    @ApiResponse(responseCode = "200", description = "Một trang bài kèm tag và số liệu nộp bài")
    @ApiResponse(responseCode = "400", description = "Tham số lọc, sắp xếp hoặc phân trang sai")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    public PageResponse<AdminProblemSummaryResponse> listProblems(
            @RequestParam(defaultValue = "PENDING_REVIEW") ProblemStatus status,
            @RequestParam(required = false) ProblemVisibility visibility,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false)
                    @Size(max = MAX_KEYWORD_LENGTH, message = "Tối đa 200 ký tự") String q,
            @RequestParam(defaultValue = "updatedAt")
                    @Pattern(
                            regexp = "updatedAt|number",
                            message = "Chỉ sắp theo updatedAt hoặc number")
                    String sort,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return adminProblemQueryService.list(
                new AdminProblemSearch(status, visibility, difficulty, tag, q, sort), page, size);
    }

    @GetMapping("/summary")
    @Operation(summary = "Số bài đã xuất bản theo phạm vi và số bài có tỉ lệ AC thấp bất thường")
    @ApiResponse(responseCode = "200", description = "Số liệu kho bài tập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    public AdminProblemBankSummaryResponse summary() {
        return adminProblemQueryService.bankSummary();
    }
}
