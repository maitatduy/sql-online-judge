package com.sqljudge.learning.dto.request;

import com.sqljudge.learning.entity.Dialect;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CellRequest(
        @NotNull(message = "Chưa chọn bộ dữ liệu") UUID datasetVariantId,
        @NotNull(message = "Chưa chọn dialect") Dialect dialect,
        @NotBlank(message = "Không được để trống") @Size(max = 10_000, message = "Tối đa 10.000 ký tự") String starterSql) {}
