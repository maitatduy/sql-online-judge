package com.sqljudge.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TransferOwnershipRequest(
        @NotBlank(message = "Nhập username người nhận") @Size(max = 50, message = "Tối đa 50 ký tự") String username) {}
