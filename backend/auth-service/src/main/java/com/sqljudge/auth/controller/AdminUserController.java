package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.dto.response.AdminUserSummaryResponse;
import com.sqljudge.auth.dto.response.UserCountsResponse;
import com.sqljudge.auth.service.AdminUserQueryService;
import com.sqljudge.auth.service.AdminUserSearch;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.web.PageResponse;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 254;

    private final AdminUserQueryService adminUserQueryService;

    @Operation(summary = "Danh sách người dùng cho admin: lọc, tìm, phân trang")
    @ApiResponse(responseCode = "200", description = "Một trang người dùng, mới tạo đứng trước")
    @ApiResponse(responseCode = "400", description = "Tham số lọc hoặc phân trang không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @GetMapping("/users")
    public PageResponse<AdminUserSummaryResponse> listUsers(
            @RequestParam(required = false)
                    @Size(max = MAX_KEYWORD_LENGTH, message = "Từ khoá quá dài") String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(defaultValue = "false") boolean deleted,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Phải từ 1 trở lên") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "Phải từ 1 đến 100") @Max(value = MAX_PAGE_SIZE, message = "Phải từ 1 đến 100") int size) {
        return adminUserQueryService.search(
                new AdminUserSearch(q, role, status, emailVerified, deleted), page, size);
    }

    @Operation(summary = "Số liệu tóm tắt người dùng cho admin")
    @ApiResponse(responseCode = "200", description = "Số tài khoản theo nhóm, không tính đã xoá")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @GetMapping("/user-summary")
    public UserCountsResponse userSummary() {
        return adminUserQueryService.countUsers();
    }
}
