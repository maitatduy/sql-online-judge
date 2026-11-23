package com.sqljudge.community.controller;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.community.dto.request.CreateReportRequest;
import com.sqljudge.community.dto.request.ResolveReportRequest;
import com.sqljudge.community.dto.response.ReportCaseDetailResponse;
import com.sqljudge.community.dto.response.ReportCaseResponse;
import com.sqljudge.community.entity.ReportStatus;
import com.sqljudge.community.entity.ReportTarget;
import com.sqljudge.community.service.ReportCaseService;
import com.sqljudge.community.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReportService reportService;
    private final ReportCaseService reportCaseService;

    @PostMapping("/api/community/reports")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Báo cáo bài viết, bình luận hoặc người dùng vi phạm")
    @ApiResponse(responseCode = "201", description = "Đã gửi báo cáo")
    @ApiResponse(responseCode = "404", description = "Không có nội dung")
    @ApiResponse(responseCode = "409", description = "Đã báo cáo nội dung này")
    public ResponseEntity<Void> report(@Valid @RequestBody CreateReportRequest request) {
        reportService.report(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/admin/reports")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Nội dung bị báo cáo, gom theo nội dung, nhiều báo cáo nhất lên đầu")
    @ApiResponse(responseCode = "200", description = "Một trang vụ báo cáo")
    @ApiResponse(responseCode = "403", description = "Không phải kiểm duyệt viên hoặc admin")
    public PageResponse<ReportCaseResponse> cases(
            @RequestParam(defaultValue = "OPEN") ReportStatus status,
            @RequestParam(required = false) ReportTarget targetType,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 50") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 50") int size) {
        return reportCaseService.cases(status, targetType, page, size);
    }

    @GetMapping("/api/admin/reports/{targetType}/{targetId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Chi tiết một vụ báo cáo: nội dung, người báo cáo, số vi phạm trước")
    @ApiResponse(responseCode = "200", description = "Chi tiết vụ báo cáo")
    @ApiResponse(responseCode = "404", description = "Không có báo cáo")
    public ReportCaseDetailResponse detail(
            @PathVariable ReportTarget targetType, @PathVariable UUID targetId) {
        return reportCaseService.detail(targetType, targetId);
    }

    @PostMapping("/api/admin/reports/{targetType}/{targetId}/resolve")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Xử lý vụ báo cáo: bỏ qua và hiện lại, hoặc gỡ nội dung")
    @ApiResponse(responseCode = "204", description = "Đã xử lý")
    @ApiResponse(responseCode = "404", description = "Không có báo cáo chờ xử lý")
    public ResponseEntity<Void> resolve(
            @PathVariable ReportTarget targetType,
            @PathVariable UUID targetId,
            @Valid @RequestBody ResolveReportRequest request) {
        reportService.resolve(targetType, targetId, request.action(), request.note());
        return ResponseEntity.noContent().build();
    }
}
