package com.sqljudge.problem.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDatasetRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Không được để trống")
                @Size(max = 120, message = "Tối đa 120 ký tự") String name,
        @Size(max = 500, message = "Tối đa 500 ký tự") String description) {}
