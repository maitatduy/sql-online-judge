package com.sqljudge.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteUserRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 500, message = "Tối đa 500 ký tự") String reason) {}
