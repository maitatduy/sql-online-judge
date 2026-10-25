package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.response.AuditLogEntryResponse;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.service.AdminAuditLogService;
import com.sqljudge.auth.service.AuditLogSearch;
import com.sqljudge.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-log")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminAuditLogService adminAuditLogService;

    @Operation(
            summary =
                    "Nhật ký thao tác của quản trị viên: lọc theo đối tượng, người, loại, thời"
                            + " gian")
    @ApiResponse(responseCode = "200", description = "Một trang nhật ký, mới nhất đứng trước")
    @ApiResponse(responseCode = "400", description = "Tham số lọc hoặc phân trang không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @GetMapping
    public PageResponse<AuditLogEntryResponse> listAuditLog(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AdminActionType action,
            @RequestParam(required = false) Instant from,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return adminAuditLogService.search(
                new AuditLogSearch(user, actor, action, from), page, size);
    }
}
