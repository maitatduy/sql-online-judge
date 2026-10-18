package com.sqljudge.auth.dto.request;

import com.sqljudge.auth.validation.NotReservedUsername;
import com.sqljudge.auth.validation.UsernameRules;
import com.sqljudge.common.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Vui lòng nhập tên người dùng") @Pattern(regexp = UsernameRules.PATTERN, message = UsernameRules.PATTERN_MESSAGE) @NotReservedUsername
                String username,
        @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") @Size(max = 254, message = "Email quá dài") String email,
        @NotNull(message = "Vui lòng chọn vai trò") Role role,
        @Size(max = 60, message = "Tên hiển thị tối đa 60 ký tự") String displayName) {}
