package com.sqljudge.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDatasetRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 120, message = "Tối đa 120 ký tự") String name,
        @Size(max = 500, message = "Tối đa 500 ký tự") String description) {}
