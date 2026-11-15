package com.sqljudge.learning.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SubmitQuizRequest(
        @NotNull(message = "Chưa có câu trả lời") @Size(max = 50, message = "Quá nhiều câu trả lời") List<@Valid Answer> answers) {

    public record Answer(
            @NotNull(message = "Thiếu câu hỏi") UUID questionKey,
            @NotNull(message = "Thiếu lựa chọn") @Size(max = 20, message = "Quá nhiều lựa chọn") List<UUID> optionKeys) {}
}
