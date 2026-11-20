package com.sqljudge.learning.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuizQuestionRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 500, message = "Tối đa 500 ký tự") String prompt,
        boolean multiple,
        @NotNull(message = "Cần từ 2 đến 6 lựa chọn") @Size(min = 2, max = 6, message = "Cần từ 2 đến 6 lựa chọn") List<@Valid QuizOptionRequest> options) {

    public long correctCount() {
        return options.stream().filter(QuizOptionRequest::correct).count();
    }
}
