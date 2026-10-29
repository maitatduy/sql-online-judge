package com.sqljudge.problem.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.problem.dto.response.ProblemProgress;
import com.sqljudge.problem.dto.response.ProblemSummaryResponse;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.service.ProblemQueryService;
import com.sqljudge.problem.service.ProblemSearch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final ProblemQueryService problemQueryService;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(
            summary =
                    "Danh sách bài đã xuất bản: lọc theo độ khó, tag, tiến độ của người xem,"
                            + " tìm theo tên hoặc số bài")
    @ApiResponse(responseCode = "200", description = "Một trang bài, xếp theo số bài")
    @ApiResponse(responseCode = "400", description = "Tham số lọc hoặc phân trang không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Gửi token nhưng token không hợp lệ")
    public PageResponse<ProblemSummaryResponse> listProblems(
            @RequestParam(required = false)
                    @Size(max = MAX_KEYWORD_LENGTH, message = "Từ khoá quá dài") String q,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) ProblemProgress progress,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return problemQueryService.listPublished(
                new ProblemSearch(q, difficulty, tag, progress), page, size);
    }
}
