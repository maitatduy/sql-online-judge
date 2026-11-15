package com.sqljudge.learning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LessonRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 160, message = "Tối đa 160 ký tự") @Pattern(
                        regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                        message = "Chỉ gồm chữ thường không dấu, số và dấu gạch nối")
                String slug,
        @NotBlank(message = "Không được để trống") @Size(max = 200, message = "Tối đa 200 ký tự") String title,
        @NotNull(message = "Không được để trống") @Size(max = 51_200, message = "Tối đa 50 KB") String body) {}
