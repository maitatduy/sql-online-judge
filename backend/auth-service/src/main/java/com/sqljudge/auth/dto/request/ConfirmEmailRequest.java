package com.sqljudge.auth.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfirmEmailRequest(
        @Size(max = 64, message = "Liên kết xác thực không hợp lệ") String token,
        @Email(message = "Email không hợp lệ") String email,
        @Pattern(regexp = "^\\d{6}$", message = "Mã xác thực gồm 6 chữ số") String code) {

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }

    @AssertTrue(message = "Cần mã xác thực hoặc liên kết trong email") public boolean isTokenOrCodeProvided() {
        return hasToken() || (email != null && !email.isBlank() && code != null);
    }
}
