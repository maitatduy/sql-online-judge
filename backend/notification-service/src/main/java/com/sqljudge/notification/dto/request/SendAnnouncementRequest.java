package com.sqljudge.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendAnnouncementRequest(
        @NotBlank(message = "Không được để trống") @Size(max = 300, message = "Tối đa 300 ký tự") String title,
        @Pattern(regexp = "/\\S*", message = "Đường dẫn phải bắt đầu bằng /") @Size(max = 300, message = "Tối đa 300 ký tự") String link) {}
