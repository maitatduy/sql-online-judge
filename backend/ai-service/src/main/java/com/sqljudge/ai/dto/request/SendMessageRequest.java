package com.sqljudge.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 4000, message = "Tối đa 4.000 ký tự") String content,
        @Size(max = 65_536, message = "Code tối đa 64 KB") String code) {}
