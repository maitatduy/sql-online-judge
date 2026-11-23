package com.sqljudge.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record WriteCommentRequest(
        UUID postId,
        UUID problemId,
        UUID parentId,
        @NotBlank(message = "Không được để trống") @Size(max = 10_000, message = "Tối đa 10.000 ký tự") String body) {}
