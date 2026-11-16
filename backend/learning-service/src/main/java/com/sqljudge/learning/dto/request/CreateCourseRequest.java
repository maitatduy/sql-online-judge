package com.sqljudge.learning.dto.request;

import com.sqljudge.learning.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 160, message = "Tối đa 160 ký tự") @Pattern(
                        regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                        message = "Chỉ gồm chữ thường không dấu, số và dấu gạch nối")
                String slug,
        @NotBlank(message = "Không được để trống") @Size(max = 200, message = "Tối đa 200 ký tự") String title,
        @NotBlank(message = "Không được để trống") @Size(max = 500, message = "Tối đa 500 ký tự") String summary,
        @NotNull(message = "Chưa chọn độ khó") Difficulty difficulty) {}
