package com.sqljudge.learning.dto.request;

import com.sqljudge.learning.entity.Dialect;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StartLessonRunRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 65_536, message = "Câu lệnh tối đa 64 KB") String sql,
        Dialect dialect) {}
