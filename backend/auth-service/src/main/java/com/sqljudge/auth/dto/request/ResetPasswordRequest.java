package com.sqljudge.auth.dto.request;

import com.sqljudge.auth.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Liên kết không hợp lệ") @Size(max = 64, message = "Liên kết không hợp lệ") String token,
        @NotBlank(message = "Vui lòng nhập mật khẩu mới") @ValidPassword String newPassword) {

    @Override
    public String toString() {
        return "ResetPasswordRequest[]";
    }
}
