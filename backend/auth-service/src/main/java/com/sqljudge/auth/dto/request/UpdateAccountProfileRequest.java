package com.sqljudge.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAccountProfileRequest(
        @NotBlank(message = "Vui lòng nhập tên hiển thị") @Size(max = 100, message = "Tối đa 100 ký tự") String displayName,
        @Size(max = 300, message = "Tối đa 300 ký tự") String bio,
        @Size(max = 120, message = "Tối đa 120 ký tự") String school,
        @Size(max = 120, message = "Tối đa 120 ký tự") String location,
        @Pattern(
                        regexp = "https?://\\S+",
                        message = "Đường dẫn phải bắt đầu bằng http:// hoặc https://")
                @Size(max = 200, message = "Tối đa 200 ký tự") String websiteUrl) {}
