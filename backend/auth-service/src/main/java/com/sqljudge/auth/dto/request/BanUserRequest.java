package com.sqljudge.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BanUserRequest(
        @NotNull(message = "Vui lòng chọn thời hạn khoá") BanDuration duration,
        @NotBlank(message = "Không được để trống") @Size(max = 500, message = "Tối đa 500 ký tự") String reason,
        @NotNull(message = "Phải là true hoặc false") Boolean notifyByEmail) {}
