package com.sqljudge.auth.dto.request;

import com.sqljudge.auth.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        String currentPassword,
        @NotBlank(message = "Vui lòng nhập mật khẩu mới") @ValidPassword String newPassword) {}
