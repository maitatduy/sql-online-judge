package com.sqljudge.contest.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.contest.dto.request.CreateContestRequest;
import com.sqljudge.contest.dto.request.ReplaceContestProblemsRequest;
import com.sqljudge.contest.dto.response.AdminContestResponse;
import com.sqljudge.contest.dto.response.ContestResponse;
import com.sqljudge.contest.dto.response.ContestSummaryResponse;
import com.sqljudge.contest.service.AdminContestService;
import com.sqljudge.contest.service.ContestProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contests")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminContestController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminContestService adminContestService;
    private final ContestProblemService contestProblemService;

    @PostMapping
    @Operation(summary = "Lên lịch một contest mới")
    @ApiResponse(responseCode = "201", description = "Contest đã tạo")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "409", description = "Slug đã có contest khác dùng")
    public ResponseEntity<ContestResponse> create(
            @Valid @RequestBody CreateContestRequest request) {
        ContestResponse contest = adminContestService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/contests/" + contest.id()))
                .body(contest);
    }

    @GetMapping
    @Operation(summary = "Mọi contest, giờ bắt đầu muộn nhất lên đầu")
    @ApiResponse(responseCode = "200", description = "Trang contest")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    public PageResponse<ContestSummaryResponse> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return adminContestService.list(page, size);
    }

    @GetMapping("/{contestId}")
    @Operation(summary = "Chi tiết một contest")
    @ApiResponse(responseCode = "200", description = "Contest")
    @ApiResponse(responseCode = "404", description = "Không có contest")
    public AdminContestResponse get(@PathVariable UUID contestId) {
        return adminContestService.get(contestId);
    }

    @PutMapping("/{contestId}/problems")
    @Operation(summary = "Thay toàn bộ danh sách bài của contest, thứ tự quyết định chữ cái A, B")
    @ApiResponse(responseCode = "200", description = "Contest với danh sách bài mới")
    @ApiResponse(responseCode = "400", description = "Quá 26 bài hoặc trùng bài")
    @ApiResponse(responseCode = "404", description = "Không có contest")
    @ApiResponse(responseCode = "409", description = "Contest đã bắt đầu")
    @ApiResponse(responseCode = "422", description = "Có bài chưa được duyệt cho contest")
    @ApiResponse(responseCode = "503", description = "Không gọi được problem-service")
    public AdminContestResponse replaceProblems(
            @PathVariable UUID contestId,
            @Valid @RequestBody ReplaceContestProblemsRequest request) {
        return contestProblemService.replace(contestId, request);
    }
}
