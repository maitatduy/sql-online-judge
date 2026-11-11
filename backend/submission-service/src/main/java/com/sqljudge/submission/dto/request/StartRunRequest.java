package com.sqljudge.submission.dto.request;

import com.sqljudge.submission.entity.Dialect;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StartRunRequest(
        @NotNull(message = "Chưa chọn dialect") Dialect dialect,
        @NotBlank(message = "Không được để trống") @Size(max = 65_536, message = "Mã nguồn tối đa 64 KB") String code) {}
