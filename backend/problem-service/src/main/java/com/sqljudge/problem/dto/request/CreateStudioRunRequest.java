package com.sqljudge.problem.dto.request;

import com.sqljudge.problem.entity.Dialect;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStudioRunRequest(
        @NotNull(message = "Chưa chọn dialect") Dialect dialect,
        @NotBlank(message = "Không được để trống") @Size(max = 65_536, message = "Câu lệnh tối đa 64 KB") String sql) {}
