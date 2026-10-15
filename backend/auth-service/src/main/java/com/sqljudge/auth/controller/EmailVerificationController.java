package com.sqljudge.auth.controller;

import com.sqljudge.auth.dto.request.ConfirmEmailRequest;
import com.sqljudge.auth.dto.request.ResendVerificationRequest;
import com.sqljudge.auth.service.EmailVerificationService;
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
@RequestMapping("/api/auth/email-verification")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "Gửi lại mã xác thực email")
    @ApiResponse(
            responseCode = "202",
            description = "Đã nhận yêu cầu, không cho biết email có tồn tại hay không")
    @ApiResponse(responseCode = "400", description = "Email sai định dạng")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resend(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerificationEmail(request.email());
    }

    @Operation(summary = "Xác thực email bằng mã 6 số hoặc liên kết trong email")
    @ApiResponse(responseCode = "204", description = "Email đã được xác thực")
    @ApiResponse(responseCode = "400", description = "Thiếu hoặc sai định dạng mã")
    @ApiResponse(responseCode = "422", description = "Mã không hợp lệ hoặc đã hết hạn")
    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@Valid @RequestBody ConfirmEmailRequest request) {
        if (request.hasToken()) {
            emailVerificationService.confirmByToken(request.token());
            return;
        }
        emailVerificationService.confirmByCode(request.email(), request.code());
    }
}
