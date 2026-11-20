package com.sqljudge.learning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewCourseRequest(
        @NotBlank(message = "Cần ghi lý do") @Size(max = 2000, message = "Tối đa 2000 ký tự") String reason) {}
