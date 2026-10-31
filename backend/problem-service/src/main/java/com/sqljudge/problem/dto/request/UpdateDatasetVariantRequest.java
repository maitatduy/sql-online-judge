package com.sqljudge.problem.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDatasetVariantRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Không được để trống")
                @Size(max = 100, message = "Tối đa 100 ký tự") String name,
        Boolean isPublic) {}
