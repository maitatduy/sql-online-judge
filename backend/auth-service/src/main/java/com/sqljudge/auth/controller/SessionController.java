package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.response.AuthResponse;
import com.sqljudge.auth.service.SessionService;
import com.sqljudge.auth.service.SignInResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @Operation(summary = "Đổi cookie refresh lấy access token mới")
    @ApiResponse(
            responseCode = "200",
            description = "Access token mới, cookie refresh đã xoay vòng")
    @ApiResponse(responseCode = "401", description = "Phiên đăng nhập không hợp lệ hoặc đã hết hạn")
    @ApiResponse(responseCode = "403", description = "Yêu cầu không đến từ trang SQL Judge")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {
        SignInResult result = sessionService.refresh(refreshToken);
        if (result.refreshToken() == null) {
            return ResponseEntity.ok(result.response());
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, RefreshTokenCookie.headerFor(result))
                .body(result.response());
    }

    @Operation(summary = "Đăng xuất: thu hồi phiên hiện tại và xoá cookie refresh")
    @ApiResponse(responseCode = "204", description = "Đã đăng xuất")
    @ApiResponse(responseCode = "403", description = "Yêu cầu không đến từ trang SQL Judge")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {
        sessionService.revoke(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, RefreshTokenCookie.cleared().toString())
                .build();
    }
}
