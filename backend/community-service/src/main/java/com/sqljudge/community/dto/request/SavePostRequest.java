package com.sqljudge.community.dto.request;

import com.sqljudge.community.entity.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SavePostRequest(
        @NotNull(message = "Chọn chuyên mục") PostCategory category,
        @NotBlank(message = "Không được để trống") @Size(min = 5, max = 200, message = "Từ 5 đến 200 ký tự") String title,
        @NotBlank(message = "Không được để trống") @Size(max = 51_200, message = "Tối đa 50 KB") String bodyMarkdown,
        UUID problemId,
        @NotNull(message = "Không được để trống") @Size(max = 5, message = "Tối đa 5 tag") List<
                                @Pattern(
                                        regexp = "[a-z0-9]+(-[a-z0-9]+)*",
                                        message = "Tag chỉ gồm chữ thường, số và dấu gạch nối")
                                @Size(max = 40, message = "Tối đa 40 ký tự") String>
                        tags,
        boolean publish) {}
