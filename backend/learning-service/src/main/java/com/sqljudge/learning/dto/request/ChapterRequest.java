package com.sqljudge.learning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChapterRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 200, message = "Tối đa 200 ký tự") String title) {}
