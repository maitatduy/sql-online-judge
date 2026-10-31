package com.sqljudge.problem.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.problem.dto.request.CreateProblemRequest;
import com.sqljudge.problem.dto.response.StudioProblemSummaryResponse;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.service.StudioProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studio/problems")
@PreAuthorize("hasAnyRole('SETTER', 'ADMIN')")
@RequiredArgsConstructor
public class StudioProblemController {

    private static final String PATH = "/api/studio/problems";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final StudioProblemService studioProblemService;

    @PostMapping
    @Operation(summary = "Tạo bài nháp mới, slug sinh từ tiêu đề")
    @ApiResponse(responseCode = "201", description = "Bài nháp vừa tạo, kèm header Location")
    @ApiResponse(responseCode = "400", description = "Tiêu đề trống hoặc quá dài")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải SETTER hoặc ADMIN")
    public ResponseEntity<StudioProblemSummaryResponse> createDraft(
            @Valid @RequestBody CreateProblemRequest request) {
        StudioProblemSummaryResponse created = studioProblemService.createDraft(request);
        return ResponseEntity.created(URI.create(PATH + "/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Bài của tôi: lọc theo trạng thái, tìm theo tên, mới sửa nhất trước")
    @ApiResponse(responseCode = "200", description = "Một trang bài do chính người gọi tạo")
    @ApiResponse(responseCode = "400", description = "Tham số lọc hoặc phân trang không hợp lệ")
    @ApiResponse(responseCode = "403", description = "Không phải SETTER hoặc ADMIN")
    public PageResponse<StudioProblemSummaryResponse> listOwned(
            @RequestParam(required = false) ProblemStatus status,
            @RequestParam(required = false)
                    @Size(max = MAX_KEYWORD_LENGTH, message = "Từ khoá quá dài") String q,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return studioProblemService.listOwned(status, q, page, size);
    }

    @GetMapping("/status-counts")
    @Operation(summary = "Số bài của tôi theo từng trạng thái, để hiện trên các tab")
    @ApiResponse(responseCode = "200", description = "Mọi trạng thái, trạng thái chưa có bài là 0")
    @ApiResponse(responseCode = "403", description = "Không phải SETTER hoặc ADMIN")
    public Map<ProblemStatus, Long> countOwnedByStatus() {
        return studioProblemService.countOwnedByStatus();
    }
}
