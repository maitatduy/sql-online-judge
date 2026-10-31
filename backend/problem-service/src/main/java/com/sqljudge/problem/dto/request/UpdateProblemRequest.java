package com.sqljudge.problem.dto.request;

import com.sqljudge.problem.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateProblemRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Không được để trống")
                @Size(max = 200, message = "Tối đa 200 ký tự") String title,
        @Pattern(
                        regexp = "[a-z0-9]+(-[a-z0-9]+)*",
                        message = "Chỉ gồm chữ thường không dấu, số và dấu gạch nối")
                @Size(max = 160, message = "Tối đa 160 ký tự") String slug,
        Difficulty difficulty,
        @Size(max = 5, message = "Tối đa 5 chủ đề") List<@NotBlank(message = "Không được để trống") String> tagSlugs,
        @Size(max = 51_200, message = "Đề bài tối đa 50 KB") String statementMarkdown) {}
