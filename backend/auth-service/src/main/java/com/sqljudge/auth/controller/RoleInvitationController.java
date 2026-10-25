package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.response.AcceptInvitationResponse;
import com.sqljudge.auth.dto.response.RoleInvitationViewResponse;
import com.sqljudge.auth.service.RoleInvitationAcceptanceService;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/role-invitations/{token}")
@RequiredArgsConstructor
public class RoleInvitationController {

    private final RoleInvitationAcceptanceService acceptanceService;

    @Operation(summary = "Xem lời mời nhận vai trò từ link trong email, không cần đăng nhập")
    @ApiResponse(
            responseCode = "200",
            description = "Email đã che bớt, vai trò, người mời, trạng thái")
    @ApiResponse(responseCode = "404", description = "Không có lời mời ứng với link")
    @GetMapping
    public RoleInvitationViewResponse view(@PathVariable String token) {
        return acceptanceService.view(token);
    }

    @Operation(summary = "Nhận lời mời: tài khoản đang đăng nhập phải đúng email được mời")
    @ApiResponse(responseCode = "200", description = "Vai trò mới; làm mới access token để dùng")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Email tài khoản khác email được mời")
    @ApiResponse(responseCode = "404", description = "Không có lời mời ứng với link")
    @ApiResponse(responseCode = "409", description = "Lời mời đã được nhận")
    @ApiResponse(
            responseCode = "422",
            description = "Lời mời hết hạn, đã huỷ hoặc là tài khoản quản trị")
    @PostMapping("/accept")
    public AcceptInvitationResponse accept(@PathVariable String token) {
        return acceptanceService.accept(token, SecurityUtils.requireCurrentUser());
    }
}
