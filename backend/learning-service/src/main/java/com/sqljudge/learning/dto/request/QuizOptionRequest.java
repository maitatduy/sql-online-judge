package com.sqljudge.learning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuizOptionRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 300, message = "Tối đa 300 ký tự") String label,
        boolean correct) {}
