package com.sqljudge.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditCommentRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 10_000, message = "Tối đa 10.000 ký tự") String body) {}
