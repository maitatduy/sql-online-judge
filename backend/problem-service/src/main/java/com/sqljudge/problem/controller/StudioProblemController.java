package com.sqljudge.problem.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.problem.dto.request.CreateProblemRequest;
import com.sqljudge.problem.dto.request.UpdateProblemRequest;
import com.sqljudge.problem.dto.response.StudioProblemResponse;
import com.sqljudge.problem.dto.response.StudioProblemSummaryResponse;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.service.StudioProblemEditService;
import com.sqljudge.problem.service.StudioProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final StudioProblemEditService studioProblemEditService;

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

    @GetMapping("/{problemId}")
    @Operation(summary = "Đọc bài để soạn trong Studio")
    @ApiResponse(
            responseCode = "200",
            description = "Bài của người gọi, hoặc bài bất kỳ nếu là ADMIN")
    @ApiResponse(
            responseCode = "404",
            description = "Không có bài hoặc không phải bài của người gọi")
    public StudioProblemResponse getProblem(@PathVariable UUID problemId) {
        return studioProblemEditService.get(problemId);
    }

    @PatchMapping("/{problemId}")
    @Operation(summary = "Sửa thông tin và đề bài; trường không gửi lên giữ nguyên")
    @ApiResponse(responseCode = "200", description = "Bài sau khi sửa")
    @ApiResponse(
            responseCode = "400",
            description = "Dữ liệu không hợp lệ hoặc chủ đề không tồn tại")
    @ApiResponse(
            responseCode = "404",
            description = "Không có bài hoặc không phải bài của người gọi")
    @ApiResponse(responseCode = "409", description = "Slug đã được bài khác dùng")
    @ApiResponse(responseCode = "422", description = "Bài đang chờ duyệt hoặc đã xuất bản")
    public StudioProblemResponse updateProblem(
            @PathVariable UUID problemId, @Valid @RequestBody UpdateProblemRequest request) {
        return studioProblemEditService.update(problemId, request);
    }
}
