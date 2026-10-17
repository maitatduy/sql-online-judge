package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.PasswordResetRequest;
import com.sqljudge.auth.dto.request.ResetPasswordRequest;
import com.sqljudge.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Operation(summary = "Gửi liên kết đặt lại mật khẩu qua email")
    @ApiResponse(
            responseCode = "202",
            description = "Đã nhận yêu cầu, không cho biết email có tồn tại hay không")
    @ApiResponse(responseCode = "400", description = "Email sai định dạng")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request.email());
    }

    @Operation(summary = "Đặt mật khẩu mới bằng liên kết trong email")
    @ApiResponse(responseCode = "204", description = "Đã đổi mật khẩu, mọi phiên bị đăng xuất")
    @ApiResponse(responseCode = "400", description = "Mật khẩu mới không hợp lệ")
    @ApiResponse(responseCode = "422", description = "Liên kết không hợp lệ hoặc đã hết hạn")
    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
    }
}
