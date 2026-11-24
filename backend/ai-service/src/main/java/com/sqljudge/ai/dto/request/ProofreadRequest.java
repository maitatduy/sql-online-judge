package com.sqljudge.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProofreadRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 20_000, message = "Tối đa 20.000 ký tự") String text) {}
