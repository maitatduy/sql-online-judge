package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.UpdateUserRequest;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.service.AdminUserEditService;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserEditController {

    private final AdminUserEditService adminUserEditService;

    @Operation(
            summary = "Sửa vai trò hoặc tên hiển thị; đổi vai trò cần lý do và đăng xuất mọi phiên",
            description =
                    "Đổi vai trò thu hồi refresh token ngay, nhưng access token đang dùng vẫn mang"
                            + " vai trò cũ tối đa 15 phút cho đến khi hết hạn.")
    @ApiResponse(responseCode = "200", description = "Chi tiết tài khoản sau khi sửa")
    @ApiResponse(responseCode = "400", description = "Thiếu trường, giá trị sai hoặc thiếu lý do")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có tài khoản hoặc đã bị xoá")
    @ApiResponse(responseCode = "422", description = "Admin tự đổi vai trò của mình")
    @PatchMapping("/{username}")
    public AdminUserDetailResponse update(
            @PathVariable String username, @Valid @RequestBody UpdateUserRequest request) {
        return adminUserEditService.update(
                username, request, SecurityUtils.requireCurrentUser().id());
    }
}
