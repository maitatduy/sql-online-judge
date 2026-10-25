package com.sqljudge.auth.dto.request;

import com.sqljudge.common.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InviteRolesRequest(
        @NotEmpty(message = "Chọn từ 1 đến 20 người") @Size(max = 20, message = "Chọn từ 1 đến 20 người") List<
                                @NotBlank(message = "Email không hợp lệ") @Email(message = "Email không hợp lệ") @Size(max = 254, message = "Email không hợp lệ") String>
                        emails,
        @NotNull(message = "Vui lòng chọn vai trò") Role role,
        @Size(max = 500, message = "Tối đa 500 ký tự") String message) {}
