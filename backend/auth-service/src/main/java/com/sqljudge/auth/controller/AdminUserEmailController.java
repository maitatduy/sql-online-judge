package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.response.UserEmailResponse;
import com.sqljudge.auth.service.AdminUserEmailService;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users/{username}")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserEmailController {

    private final AdminUserEmailService adminUserEmailService;

    @Operation(summary = "Gửi link đặt lại mật khẩu tới email của tài khoản, ghi nhật ký")
    @ApiResponse(responseCode = "202", description = "Đã nhận, email được gửi sau khi lưu")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có tài khoản hoặc đã bị xoá")
    @ApiResponse(responseCode = "429", description = "Vừa gửi trong 60 giây, kèm Retry-After")
    @PostMapping("/password-reset-email")
    public ResponseEntity<UserEmailResponse> sendPasswordResetEmail(@PathVariable String username) {
        return ResponseEntity.accepted()
                .body(
                        adminUserEmailService.sendPasswordResetEmail(
                                username, SecurityUtils.requireCurrentUser().id()));
    }

    @Operation(summary = "Gửi lại mã xác minh email cho tài khoản chưa xác minh, ghi nhật ký")
    @ApiResponse(responseCode = "202", description = "Đã nhận, email được gửi sau khi lưu")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    @ApiResponse(responseCode = "404", description = "Không có tài khoản hoặc đã bị xoá")
    @ApiResponse(responseCode = "409", description = "Email đã được xác minh")
    @ApiResponse(responseCode = "429", description = "Vừa gửi trong 60 giây, kèm Retry-After")
    @PostMapping("/verification-email")
    public ResponseEntity<UserEmailResponse> sendVerificationEmail(@PathVariable String username) {
        return ResponseEntity.accepted()
                .body(
                        adminUserEmailService.sendVerificationEmail(
                                username, SecurityUtils.requireCurrentUser().id()));
    }
}
