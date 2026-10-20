package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.BulkBanUsersRequest;
import com.sqljudge.auth.dto.request.BulkChangeRoleRequest;
import com.sqljudge.auth.dto.response.BulkUserActionResponse;
import com.sqljudge.auth.service.AdminUserBulkService;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserBulkController {

    private final AdminUserBulkService adminUserBulkService;

    @Operation(
            summary = "Đổi vai trò tối đa 100 tài khoản trong một lần, mỗi tài khoản bị đăng xuất",
            description =
                    "Bỏ qua chính mình, tài khoản admin, tài khoản không tồn tại hoặc đã xoá, tài"
                            + " khoản đã đúng vai trò. Không cấp ADMIN hàng loạt.")
    @ApiResponse(responseCode = "200", description = "Số tài khoản đã đổi và danh sách bị bỏ qua")
    @ApiResponse(responseCode = "400", description = "Danh sách, vai trò hoặc lý do không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @PostMapping("/bulk-role")
    public BulkUserActionResponse changeRole(@Valid @RequestBody BulkChangeRoleRequest request) {
        return adminUserBulkService.changeRole(request, SecurityUtils.requireCurrentUser().id());
    }

    @Operation(
            summary = "Khoá tối đa 100 tài khoản trong một lần, như khoá từng tài khoản",
            description =
                    "Mỗi tài khoản bị khoá được đăng xuất mọi phiên, ghi nhật ký và nhận email nếu"
                            + " chọn. Bỏ qua chính mình, tài khoản admin, tài khoản đang bị khoá,"
                            + " không tồn tại hoặc đã xoá.")
    @ApiResponse(responseCode = "200", description = "Số tài khoản đã khoá và danh sách bị bỏ qua")
    @ApiResponse(responseCode = "400", description = "Danh sách, thời hạn, lý do hoặc tuỳ chọn sai")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @PostMapping("/bulk-ban")
    public BulkUserActionResponse ban(@Valid @RequestBody BulkBanUsersRequest request) {
        return adminUserBulkService.ban(request, SecurityUtils.requireCurrentUser().id());
    }
}
