package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.RegisterRequest;
import com.sqljudge.auth.dto.response.RegisterResponse;
import com.sqljudge.auth.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;

    @Operation(summary = "Đăng ký tài khoản bằng email, gửi mã xác thực")
    @ApiResponse(responseCode = "201", description = "Đã tạo tài khoản chưa xác thực")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "409", description = "Username hoặc email đã có người dùng")
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        URI location =
                UriComponentsBuilder.fromPath("/api/users/{username}")
                        .buildAndExpand(request.username())
                        .toUri();
        return ResponseEntity.created(location).body(registrationService.register(request));
    }
}
