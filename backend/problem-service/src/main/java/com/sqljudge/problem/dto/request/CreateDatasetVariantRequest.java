package com.sqljudge.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDatasetVariantRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 100, message = "Tối đa 100 ký tự") String name,
        boolean isPublic) {}
