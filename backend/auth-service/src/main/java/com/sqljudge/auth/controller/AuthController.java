package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.LoginRequest;
import com.sqljudge.auth.dto.request.RegisterRequest;
import com.sqljudge.auth.dto.response.AuthResponse;
import com.sqljudge.auth.dto.response.CurrentUserResponse;
import com.sqljudge.auth.dto.response.RegisterResponse;
import com.sqljudge.auth.service.LoginService;
import com.sqljudge.auth.service.RegistrationService;
import com.sqljudge.auth.service.SignInResult;
import com.sqljudge.auth.service.UserService;
import com.sqljudge.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final UserService userService;

    @Operation(summary = "Đăng ký tài khoản bằng email, gửi mã xác thực")
    @ApiResponse(
            responseCode = "201",
            description =
                    "Đã nhận đăng ký. Email đã có tài khoản cũng trả 201 để không lộ email, chủ"
                            + " email nhận mail báo")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "409", description = "Username đã có người dùng")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return registrationService.register(request);
    }

    @Operation(summary = "Đăng nhập bằng email và mật khẩu")
    @ApiResponse(responseCode = "200", description = "Access token, kèm cookie refresh")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Sai email hoặc mật khẩu")
    @ApiResponse(responseCode = "403", description = "Email chưa xác thực hoặc tài khoản bị khoá")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        SignInResult result = loginService.login(request, userAgent);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, RefreshTokenCookie.headerFor(result))
                .body(result.response());
    }

    @Operation(summary = "Người dùng đang đăng nhập")
    @ApiResponse(responseCode = "200", description = "Thông tin người dùng hiện tại")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @GetMapping("/me")
    public CurrentUserResponse me() {
        return userService.getCurrentUser(SecurityUtils.requireCurrentUser().id());
    }
}
