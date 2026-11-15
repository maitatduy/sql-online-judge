package com.sqljudge.learning.dto.request;

import com.sqljudge.learning.entity.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCourseRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 200, message = "Tối đa 200 ký tự") String title,
        @NotBlank(message = "Không được để trống") @Size(max = 500, message = "Tối đa 500 ký tự") String summary,
        @NotNull(message = "Chưa chọn độ khó") Difficulty difficulty,
        @Min(value = 1, message = "Từ 1 đến 6000 phút") @Max(value = 6000, message = "Từ 1 đến 6000 phút") int estimatedMinutes) {}
