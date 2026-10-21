package com.sqljudge.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddUserNoteRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 1000, message = "Tối đa 1000 ký tự") String body) {

    public AddUserNoteRequest {
        body = body == null ? null : body.strip();
    }
}
