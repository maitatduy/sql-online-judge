package com.sqljudge.problem.dto.request;

import com.sqljudge.problem.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuickEditProblemRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 200, message = "Tối đa 200 ký tự") String title,
        @NotNull(message = "Chọn độ khó") Difficulty difficulty,
        @NotNull(message = "Chọn chủ đề") @Size(max = 10, message = "Tối đa 10 chủ đề") List<String> tagSlugs) {}
